idf_component_register(SRCS "app_main.c"
                    PRIV_REQUIRES mqtt
                                  nvs_flash
                                  esp_netif
                                  esp_wifi
                                  esp_event
                                  esp_http_server
                                  driver
                                  espressif__qrcode
                                  json
                    INCLUDE_DIRS ".")
