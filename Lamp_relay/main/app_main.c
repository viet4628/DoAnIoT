/**
 * @brief ESP32 Smart Home - Multi-Relay + QR Setup for Flutter + MQTT
 */

#include <stdio.h>
#include <stdint.h>
#include <stddef.h>
#include <string.h>
#include <stdlib.h>

#include "esp_wifi.h"
#include "esp_system.h"
#include "esp_mac.h"
#include "nvs_flash.h"
#include "nvs.h"
#include "esp_event.h"
#include "esp_netif.h"
#include "esp_http_server.h"
#include "esp_log.h"
#include "mqtt_client.h"
#include "driver/gpio.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/event_groups.h"
#include "qrcode.h"
#include "cJSON.h"

static const char *TAG = "SMARTHOME";

typedef struct {
    uint8_t id;
    const char *name;
    gpio_num_t gpio_relay;      // GPIO for relay output
    gpio_num_t gpio_switch;     // GPIO for physical switch input
    uint8_t state;
    uint8_t last_switch_state;  // Track physical switch state
    char topic_ctrl[40];
    char topic_status[40];
} relay_device_t;

// Physical switches connected to GPIO inputs (with pull-up, active LOW)
static relay_device_t s_devices[] = {
    { .id = 1, .name = "Den phong khach", .gpio_relay = GPIO_NUM_2,  .gpio_switch = GPIO_NUM_15 },
    { .id = 2, .name = "Quat",            .gpio_relay = GPIO_NUM_4,  .gpio_switch = GPIO_NUM_16 },
    { .id = 3, .name = "Den ngu",         .gpio_relay = GPIO_NUM_5,  .gpio_switch = GPIO_NUM_17 },
    { .id = 4, .name = "Bom nuoc",        .gpio_relay = GPIO_NUM_18, .gpio_switch = GPIO_NUM_19 },
};

#define NUM_DEVICES (sizeof(s_devices) / sizeof(s_devices[0]))
#define RESET_BTN_GPIO GPIO_NUM_0
#define RESET_HOLD_MS 5000

#define NVS_NAMESPACE "smarthome"
#define NVS_KEY_SSID "wifi_ssid"
#define NVS_KEY_PASS "wifi_pass"
#define NVS_KEY_BROKER "mqtt_broker"

#define WIFI_CONNECTED_BIT BIT0
#define WIFI_FAIL_BIT BIT1

#define PROV_AP_PREFIX "SMARTHOME_"
#define PROV_AP_PASS ""
#define PROV_AP_MAX_CONN 4
#define PROV_ENDPOINT "http://192.168.4.1/provision"
#define PROV_SCAN_ENDPOINT "http://192.168.4.1/scan"
#define PROV_POP_PIN "HUTECHIOT123"

static EventGroupHandle_t s_wifi_event_group;
static esp_mqtt_client_handle_t s_mqtt_client = NULL;
static uint32_t s_mqtt_connected = 0;
static httpd_handle_t s_httpd = NULL;
static char s_prov_ap_ssid[32] = {0};

// --- WiFi Scan cache (dùng task riêng để tránh block httpd) ---
#define SCAN_MAX_APS 20
static wifi_ap_record_t s_scan_results[SCAN_MAX_APS];
static uint16_t         s_scan_ap_count = 0;
static SemaphoreHandle_t s_scan_done_sem = NULL;

static bool is_mqtt_uri(const char *value)
{
    if (!value) return false;
    return strncmp(value, "mqtt://", 7) == 0 || strncmp(value, "mqtts://", 8) == 0;
}

static void broker_to_uri(const char *input, char *out_uri, size_t out_len)
{
    if (!input || !out_uri || out_len == 0) {
        return;
    }

    memset(out_uri, 0, out_len);
    if (is_mqtt_uri(input)) {
        strlcpy(out_uri, input, out_len);
        return;
    }

    if (strchr(input, ':') != NULL) {
        snprintf(out_uri, out_len, "mqtt://%s", input);
    } else {
        snprintf(out_uri, out_len, "mqtt://%s:1883", input);
    }
}

static esp_err_t nvs_read_str(const char *key, char *buf, size_t buf_len)
{
    nvs_handle_t h;
    esp_err_t err = nvs_open(NVS_NAMESPACE, NVS_READONLY, &h);
    if (err != ESP_OK) return err;
    err = nvs_get_str(h, key, buf, &buf_len);
    nvs_close(h);
    return err;
}

static esp_err_t nvs_write_str(const char *key, const char *val)
{
    nvs_handle_t h;
    esp_err_t err = nvs_open(NVS_NAMESPACE, NVS_READWRITE, &h);
    if (err != ESP_OK) return err;
    err = nvs_set_str(h, key, val);
    if (err == ESP_OK) err = nvs_commit(h);
    nvs_close(h);
    return err;
}

static void cfg_erase_all(void)
{
    nvs_handle_t h;
    if (nvs_open(NVS_NAMESPACE, NVS_READWRITE, &h) == ESP_OK) {
        nvs_erase_all(h);
        nvs_commit(h);
        nvs_close(h);
    }
}

static void devices_init(void)
{
    for (int i = 0; i < (int)NUM_DEVICES; i++) {
        relay_device_t *d = &s_devices[i];
        d->state = 1;
        snprintf(d->topic_ctrl, sizeof(d->topic_ctrl), "/home/relay/%d/control", d->id);
        snprintf(d->topic_status, sizeof(d->topic_status), "/home/relay/%d/status", d->id);

        // Initialize relay GPIO (output)
        gpio_reset_pin(d->gpio_relay);
        gpio_set_direction(d->gpio_relay, GPIO_MODE_OUTPUT);
        gpio_set_level(d->gpio_relay, d->state);

        // Initialize physical switch GPIO (input with pull-up)
        gpio_reset_pin(d->gpio_switch);
        gpio_set_direction(d->gpio_switch, GPIO_MODE_INPUT);
        gpio_set_pull_mode(d->gpio_switch, GPIO_PULLUP_ONLY);
        d->last_switch_state = gpio_get_level(d->gpio_switch);

        ESP_LOGI(TAG, "Device[%d] '%s' Relay=GPIO%d Switch=GPIO%d topics=%s|%s",
                 d->id, d->name, d->gpio_relay, d->gpio_switch, d->topic_ctrl, d->topic_status);
    }
}

static void relay_set(relay_device_t *d, uint8_t level)
{
    d->state = level;
    gpio_set_level(d->gpio_relay, d->state);
    ESP_LOGI(TAG, "Relay[%d] '%s' -> %s", d->id, d->name, level == 0 ? "ON" : "OFF");
}

static void publish_status(relay_device_t *d)
{
    if (!s_mqtt_connected || !s_mqtt_client) return;
    const char *payload = d->state == 0 ? "0" : "1";
    esp_mqtt_client_publish(s_mqtt_client, d->topic_status, payload, 0, 1, 1);
    ESP_LOGI(TAG, "Published %s -> %s", d->topic_status, payload);
}

static void publish_all_status(void)
{
    for (int i = 0; i < (int)NUM_DEVICES; i++) {
        publish_status(&s_devices[i]);
    }
}

static void reset_button_task(void *arg)
{
    gpio_reset_pin(RESET_BTN_GPIO);
    gpio_set_direction(RESET_BTN_GPIO, GPIO_MODE_INPUT);
    gpio_set_pull_mode(RESET_BTN_GPIO, GPIO_PULLUP_ONLY);

    while (1) {
        if (gpio_get_level(RESET_BTN_GPIO) == 0) {
            vTaskDelay(pdMS_TO_TICKS(RESET_HOLD_MS));
            if (gpio_get_level(RESET_BTN_GPIO) == 0) {
                ESP_LOGW(TAG, "BOOT held - erasing config, restarting");
                cfg_erase_all();
                vTaskDelay(pdMS_TO_TICKS(500));
                esp_restart();
            }
        }
        vTaskDelay(pdMS_TO_TICKS(100));
    }
}

// Monitor physical switches and sync with relay state
static void physical_switch_monitor_task(void *arg)
{
    ESP_LOGI(TAG, "Physical switch monitor started");
    
    while (1) {
        for (int i = 0; i < (int)NUM_DEVICES; i++) {
            relay_device_t *d = &s_devices[i];
            
            // Read current physical switch state (active LOW with pull-up)
            uint8_t current_switch = gpio_get_level(d->gpio_switch);
            
            // Detect ANY state change (for toggle switch: both ON→OFF and OFF→ON)
            if (current_switch != d->last_switch_state) {
                vTaskDelay(pdMS_TO_TICKS(200)); // Debounce delay
                current_switch = gpio_get_level(d->gpio_switch); // Re-read
                
                // Confirm state has really changed (not just noise)
                if (current_switch != d->last_switch_state) {
                    d->last_switch_state = current_switch;
                    
                    // Toggle relay state
                    uint8_t new_state = (d->state == 0) ? 1 : 0;
                    relay_set(d, new_state);
                    publish_status(d);
                    
                    ESP_LOGI(TAG, "Physical switch[%d] changed (GPIO=%d) -> Relay %s",
                             d->id, current_switch, new_state == 0 ? "ON" : "OFF");
                }
            }
        }
        
        vTaskDelay(pdMS_TO_TICKS(50)); // Check every 50ms
    }
}

static void generate_provision_ap_ssid(void)
{
    uint8_t mac[6] = {0};
    esp_read_mac(mac, ESP_MAC_WIFI_SOFTAP);
    snprintf(s_prov_ap_ssid, sizeof(s_prov_ap_ssid), "%s%02X%02X%02X",
             PROV_AP_PREFIX, mac[3], mac[4], mac[5]);
}

static void print_provisioning_qr(void)
{
    char payload[384] = {0};
    snprintf(payload, sizeof(payload),
             "{\"type\":\"smarthome-provision\",\"apSsid\":\"%s\",\"apPassword\":\"%s\",\"endpoint\":\"%s\",\"method\":\"POST\",\"pop\":\"%s\"}",
             s_prov_ap_ssid,
             PROV_AP_PASS,
             PROV_ENDPOINT,
             PROV_POP_PIN);

    ESP_LOGI(TAG, "================================================");
    ESP_LOGI(TAG, "SCANNABLE QR CODE FOR PROVISIONING:");
    esp_qrcode_config_t qr_cfg = ESP_QRCODE_CONFIG_DEFAULT();
    esp_qrcode_generate(&qr_cfg, payload);
    ESP_LOGI(TAG, "QR Payload: %s", payload);
    ESP_LOGI(TAG, "PROVISIONING PIN: %s", PROV_POP_PIN);
    ESP_LOGI(TAG, "================================================");
}

static void mqtt_event_handler(void *handler_args,
                               esp_event_base_t base,
                               int32_t event_id,
                               void *event_data)
{
    (void)handler_args;
    (void)base;

    esp_mqtt_event_handle_t event = event_data;
    switch ((esp_mqtt_event_id_t)event_id) {
        case MQTT_EVENT_CONNECTED:
            ESP_LOGI(TAG, "MQTT connected");
            s_mqtt_connected = 1;
            for (int i = 0; i < (int)NUM_DEVICES; i++) {
                esp_mqtt_client_subscribe(s_mqtt_client, s_devices[i].topic_ctrl, 1);
                ESP_LOGI(TAG, "Subscribed: %s", s_devices[i].topic_ctrl);
            }
            publish_all_status();
            break;

        case MQTT_EVENT_DISCONNECTED:
            ESP_LOGI(TAG, "MQTT disconnected");
            s_mqtt_connected = 0;
            break;

        case MQTT_EVENT_DATA:
            if (event->topic_len <= 0 || event->data_len <= 0) break;
            for (int i = 0; i < (int)NUM_DEVICES; i++) {
                relay_device_t *d = &s_devices[i];
                if (strncmp(event->topic, d->topic_ctrl, event->topic_len) == 0) {
                    if (event->data[0] == '0') {
                        relay_set(d, 0);
                    } else if (event->data[0] == '1') {
                        relay_set(d, 1);
                    }
                    publish_status(d);
                    break;
                }
            }
            break;

        default:
            break;
    }
}

static void mqtt_start(const char *broker_uri)
{
    ESP_LOGI(TAG, "Starting MQTT: %s", broker_uri);
    esp_mqtt_client_config_t cfg = {
        .broker.address.uri = broker_uri,
    };
    s_mqtt_client = esp_mqtt_client_init(&cfg);
    esp_mqtt_client_register_event(s_mqtt_client, ESP_EVENT_ANY_ID, mqtt_event_handler, NULL);
    esp_mqtt_client_start(s_mqtt_client);
}

static void wifi_sta_event_handler(void *arg,
                                   esp_event_base_t event_base,
                                   int32_t event_id,
                                   void *event_data)
{
    static int retries = 0;

    if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_START) {
        esp_wifi_connect();
    } else if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_DISCONNECTED) {
        if (retries < 10) {
            retries++;
            ESP_LOGW(TAG, "Wi-Fi retry %d/10", retries);
            esp_wifi_connect();
        } else {
            xEventGroupSetBits(s_wifi_event_group, WIFI_FAIL_BIT);
        }
    } else if (event_base == IP_EVENT && event_id == IP_EVENT_STA_GOT_IP) {
        ip_event_got_ip_t *e = (ip_event_got_ip_t *)event_data;
        ESP_LOGI(TAG, "Got IP: " IPSTR, IP2STR(&e->ip_info.ip));
        retries = 0;
        xEventGroupSetBits(s_wifi_event_group, WIFI_CONNECTED_BIT);
    }
}

static bool wifi_connect_sta(const char *ssid, const char *pass)
{
    wifi_config_t cfg = {0};
    strlcpy((char *)cfg.sta.ssid, ssid, sizeof(cfg.sta.ssid));
    strlcpy((char *)cfg.sta.password, pass, sizeof(cfg.sta.password));
    cfg.sta.threshold.authmode = WIFI_AUTH_WPA2_PSK;

    ESP_ERROR_CHECK(esp_event_handler_register(WIFI_EVENT, ESP_EVENT_ANY_ID, &wifi_sta_event_handler, NULL));
    ESP_ERROR_CHECK(esp_event_handler_register(IP_EVENT, IP_EVENT_STA_GOT_IP, &wifi_sta_event_handler, NULL));

    ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_STA));
    ESP_ERROR_CHECK(esp_wifi_set_config(WIFI_IF_STA, &cfg));
    ESP_ERROR_CHECK(esp_wifi_start());

    EventBits_t bits = xEventGroupWaitBits(
        s_wifi_event_group,
        WIFI_CONNECTED_BIT | WIFI_FAIL_BIT,
        pdFALSE,
        pdFALSE,
        pdMS_TO_TICKS(20000)
    );

    esp_event_handler_unregister(WIFI_EVENT, ESP_EVENT_ANY_ID, &wifi_sta_event_handler);
    esp_event_handler_unregister(IP_EVENT, IP_EVENT_STA_GOT_IP, &wifi_sta_event_handler);

    return (bits & WIFI_CONNECTED_BIT) != 0;
}

static void set_cors_headers(httpd_req_t *req)
{
    httpd_resp_set_hdr(req, "Access-Control-Allow-Origin", "*");
    httpd_resp_set_hdr(req, "Access-Control-Allow-Methods", "GET,POST,OPTIONS");
    httpd_resp_set_hdr(req, "Access-Control-Allow-Headers", "Content-Type");
}

static esp_err_t handler_options(httpd_req_t *req)
{
    set_cors_headers(req);
    httpd_resp_set_status(req, "204 No Content");
    return httpd_resp_send(req, NULL, 0);
}

static esp_err_t handler_info(httpd_req_t *req)
{
    set_cors_headers(req);

    cJSON *root = cJSON_CreateObject();
    cJSON_AddStringToObject(root, "type", "smarthome-provision");
    cJSON_AddStringToObject(root, "apSsid", s_prov_ap_ssid);
    cJSON_AddStringToObject(root, "endpoint", PROV_ENDPOINT);
    cJSON_AddStringToObject(root, "method", "POST");

    char *json = cJSON_PrintUnformatted(root);
    cJSON_Delete(root);

    if (!json) {
        httpd_resp_send_500(req);
        return ESP_FAIL;
    }

    httpd_resp_set_type(req, "application/json");
    httpd_resp_send(req, json, HTTPD_RESP_USE_STRLEN);
    free(json);
    return ESP_OK;
}

static esp_err_t handler_provision(httpd_req_t *req)
{
    set_cors_headers(req);

    char body[512] = {0};
    int total = req->content_len;
    if (total <= 0 || total >= (int)sizeof(body)) {
        httpd_resp_set_status(req, "400 Bad Request");
        return httpd_resp_send(req, "Invalid body", HTTPD_RESP_USE_STRLEN);
    }

    int read = httpd_req_recv(req, body, total);
    if (read <= 0) {
        httpd_resp_send_500(req);
        return ESP_FAIL;
    }
    body[read] = '\0';

    cJSON *root = cJSON_Parse(body);
    if (!root) {
        httpd_resp_set_status(req, "400 Bad Request");
        return httpd_resp_send(req, "Invalid JSON", HTTPD_RESP_USE_STRLEN);
    }

    cJSON *ssid = cJSON_GetObjectItemCaseSensitive(root, "ssid");
    cJSON *pass = cJSON_GetObjectItemCaseSensitive(root, "password");
    cJSON *broker = cJSON_GetObjectItemCaseSensitive(root, "broker");
    cJSON *pop = cJSON_GetObjectItemCaseSensitive(root, "pop");

    if (!cJSON_IsString(pop) || !pop->valuestring || strcmp(pop->valuestring, PROV_POP_PIN) != 0) {
        cJSON_Delete(root);
        httpd_resp_set_status(req, "403 Forbidden");
        return httpd_resp_send(req, "Invalid POP PIN", HTTPD_RESP_USE_STRLEN);
    }

    if (!cJSON_IsString(ssid) || !ssid->valuestring || strlen(ssid->valuestring) == 0) {
        cJSON_Delete(root);
        httpd_resp_set_status(req, "400 Bad Request");
        return httpd_resp_send(req, "Missing ssid", HTTPD_RESP_USE_STRLEN);
    }

    char broker_uri[96] = {0};
    if (cJSON_IsString(broker) && broker->valuestring && strlen(broker->valuestring) > 0) {
        broker_to_uri(broker->valuestring, broker_uri, sizeof(broker_uri));
    } else {
        strlcpy(broker_uri, CONFIG_BROKER_URL, sizeof(broker_uri));
    }

    char saved_ssid[64] = {0};
    strlcpy(saved_ssid, ssid->valuestring, sizeof(saved_ssid));

    nvs_write_str(NVS_KEY_SSID, saved_ssid);
    nvs_write_str(NVS_KEY_PASS, (cJSON_IsString(pass) && pass->valuestring) ? pass->valuestring : "");
    nvs_write_str(NVS_KEY_BROKER, broker_uri);

    cJSON_Delete(root);

    ESP_LOGI(TAG, "Provision saved. SSID='%s' broker='%s'", saved_ssid, broker_uri);
    httpd_resp_send(req, "OK", HTTPD_RESP_USE_STRLEN);

    vTaskDelay(pdMS_TO_TICKS(1200));
    esp_restart();
    return ESP_OK;
}

// Task riêng để thực hiện WiFi scan blocking (tránh overflow task httpd)
static void wifi_scan_task(void *arg)
{
    ESP_LOGI(TAG, "[SCAN] Task started");

    s_scan_ap_count = 0;
    memset(s_scan_results, 0, sizeof(s_scan_results));

    // Dùng NULL (default config) để tương thích tối đa
    esp_err_t ret = esp_wifi_scan_start(NULL, true);
    ESP_LOGI(TAG, "[SCAN] scan_start ret=0x%x (%s)", ret, esp_err_to_name(ret));

    if (ret == ESP_OK) {
        // ⚠️ QUAN TRỌNG: get_ap_num TRƯỚC, get_ap_records SAU
        // (get_ap_records sẽ xóa danh sách nội bộ sau khi gọi)
        uint16_t total_found = 0;
        esp_wifi_scan_get_ap_num(&total_found);
        ESP_LOGI(TAG, "[SCAN] Total APs found by driver: %d", total_found);

        uint16_t number = SCAN_MAX_APS;
        esp_wifi_scan_get_ap_records(&number, s_scan_results);
        ESP_LOGI(TAG, "[SCAN] Records written to buffer: %d", number);

        s_scan_ap_count = number; // dùng 'number' vì nó chứa count thực tế
    } else {
        ESP_LOGE(TAG, "[SCAN] scan_start failed");
    }

    ESP_LOGI(TAG, "[SCAN] Done. Cached %d APs", s_scan_ap_count);
    for (int i = 0; i < s_scan_ap_count; i++) {
        ESP_LOGI(TAG, "[SCAN]  [%d] SSID='%s' RSSI=%d",
                 i, s_scan_results[i].ssid, s_scan_results[i].rssi);
    }

    if (s_scan_done_sem) xSemaphoreGive(s_scan_done_sem);
    vTaskDelete(NULL);
}

static esp_err_t handler_scan(httpd_req_t *req)
{
    set_cors_headers(req);
    ESP_LOGI(TAG, "WiFi scan requested... serving %d cached APs", s_scan_ap_count);

    // Nếu chưa có cache (phường hợp hiếm), thử scan lại một lần
    if (s_scan_ap_count == 0) {
        ESP_LOGW(TAG, "Cache empty, triggering rescan...");
        if (!s_scan_done_sem) {
            s_scan_done_sem = xSemaphoreCreateBinary();
        }
        xTaskCreate(wifi_scan_task, "wifi_scan", 4096, NULL, 5, NULL);
        xSemaphoreTake(s_scan_done_sem, pdMS_TO_TICKS(12000));
    }

    // Xây dựng JSON từ kết quả cache
    cJSON *root = cJSON_CreateObject();
    cJSON *list = cJSON_CreateArray();
    for (int i = 0; i < s_scan_ap_count; i++) {
        if (strlen((char *)s_scan_results[i].ssid) == 0) continue;
        cJSON *item = cJSON_CreateObject();
        cJSON_AddStringToObject(item, "ssid",   (char *)s_scan_results[i].ssid);
        cJSON_AddNumberToObject(item, "rssi",   s_scan_results[i].rssi);
        cJSON_AddBoolToObject  (item, "secure", s_scan_results[i].authmode != WIFI_AUTH_OPEN);
        cJSON_AddItemToArray(list, item);
    }
    cJSON_AddItemToObject(root, "wifi_list", list);

    char *json = cJSON_PrintUnformatted(root);
    cJSON_Delete(root);

    httpd_resp_set_type(req, "application/json");
    if (json) {
        httpd_resp_sendstr(req, json);
        free(json);
    } else {
        httpd_resp_sendstr(req, "{\"wifi_list\":[]}");
    }
    return ESP_OK;
}

static void start_provisioning_server(void)
{
    httpd_config_t cfg = HTTPD_DEFAULT_CONFIG();
    cfg.max_uri_handlers = 8;
    cfg.stack_size = 8192; // Tăng stack để tránh overflow khi scan WiFi blocking
    if (httpd_start(&s_httpd, &cfg) != ESP_OK) {
        ESP_LOGE(TAG, "Failed to start HTTP server");
        return;
    }

    httpd_uri_t uri_info = { .uri = "/info", .method = HTTP_GET, .handler = handler_info };
    httpd_uri_t uri_provision = { .uri = "/provision", .method = HTTP_POST, .handler = handler_provision };
    httpd_uri_t uri_provision_opts = { .uri = "/provision", .method = HTTP_OPTIONS, .handler = handler_options };
    httpd_uri_t uri_scan = { .uri = "/scan", .method = HTTP_GET, .handler = handler_scan };

    httpd_register_uri_handler(s_httpd, &uri_info);
    httpd_register_uri_handler(s_httpd, &uri_provision);
    httpd_register_uri_handler(s_httpd, &uri_provision_opts);
    httpd_register_uri_handler(s_httpd, &uri_scan);
}

static void start_ap_provisioning(void)
{
    generate_provision_ap_ssid();
    esp_netif_create_default_wifi_ap();

    wifi_config_t ap_cfg = {
        .ap = {
            .ssid_len = 0,
            .max_connection = PROV_AP_MAX_CONN,
            .authmode = WIFI_AUTH_OPEN,
        },
    };
    strlcpy((char *)ap_cfg.ap.ssid, s_prov_ap_ssid, sizeof(ap_cfg.ap.ssid));
    strlcpy((char *)ap_cfg.ap.password, PROV_AP_PASS, sizeof(ap_cfg.ap.password));

    ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_APSTA));
    ESP_ERROR_CHECK(esp_wifi_set_config(WIFI_IF_AP, &ap_cfg));
    ESP_ERROR_CHECK(esp_wifi_start());

    // Pre-scan WiFi TRƯỜC khi phone kết nối vào AP
    // Radio đang rảnh → scan hiệu quả nhất
    ESP_LOGI(TAG, "Pre-scanning WiFi before AP clients join...");
    if (!s_scan_done_sem) {
        s_scan_done_sem = xSemaphoreCreateBinary();
    }
    xTaskCreate(wifi_scan_task, "wifi_prescan", 4096, NULL, 5, NULL);
    // Chờ scan xong (tối đa 12 giây) mới in QR và khởi động server
    xSemaphoreTake(s_scan_done_sem, pdMS_TO_TICKS(12000));
    ESP_LOGI(TAG, "Pre-scan complete: %d networks cached", s_scan_ap_count);

    ESP_LOGI(TAG, "Provisioning AP started. SSID='%s'", s_prov_ap_ssid);
    start_provisioning_server();
    print_provisioning_qr();
}

void app_main(void)
{
    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        ESP_ERROR_CHECK(nvs_flash_erase());
        ret = nvs_flash_init();
    }
    ESP_ERROR_CHECK(ret);

    devices_init();
    xTaskCreate(reset_button_task, "reset_btn", 2048, NULL, 5, NULL);
    xTaskCreate(physical_switch_monitor_task, "phys_switch", 4096, NULL, 5, NULL);

    s_wifi_event_group = xEventGroupCreate();

    ESP_ERROR_CHECK(esp_netif_init());
    ESP_ERROR_CHECK(esp_event_loop_create_default());
    esp_netif_create_default_wifi_sta();

    wifi_init_config_t wifi_cfg = WIFI_INIT_CONFIG_DEFAULT();
    ESP_ERROR_CHECK(esp_wifi_init(&wifi_cfg));

    char saved_ssid[64] = {0};
    char saved_pass[64] = {0};
    char saved_broker[96] = {0};

    bool has_config =
        (nvs_read_str(NVS_KEY_SSID, saved_ssid, sizeof(saved_ssid)) == ESP_OK) &&
        (nvs_read_str(NVS_KEY_BROKER, saved_broker, sizeof(saved_broker)) == ESP_OK) &&
        (strlen(saved_ssid) > 0) && (strlen(saved_broker) > 0);

    nvs_read_str(NVS_KEY_PASS, saved_pass, sizeof(saved_pass));

    if (!has_config) {
        ESP_LOGI(TAG, "No config found, entering app-driven QR provisioning mode");
        start_ap_provisioning();
        while (1) {
            vTaskDelay(pdMS_TO_TICKS(1000));
        }
    }

    ESP_LOGI(TAG, "Config found: ssid='%s' broker='%s'", saved_ssid, saved_broker);

    bool connected = wifi_connect_sta(saved_ssid, saved_pass);
    if (!connected) {
        ESP_LOGE(TAG, "Wi-Fi failed - clearing config, restarting");
        cfg_erase_all();
        vTaskDelay(pdMS_TO_TICKS(500));
        esp_restart();
    }

    mqtt_start(saved_broker);
    while (1) {
        vTaskDelay(pdMS_TO_TICKS(5000));
    }
}