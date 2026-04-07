# Smart Home Web Dashboard

ReactJS web dashboard for controlling IoT devices and managing automation schedules.

## Features

- **Device Control**: Real-time device monitoring and control
- **Automation Management**: Create, edit, and toggle automation schedules
- **Responsive Design**: Works on desktop, tablet, and mobile browsers
- **Auto-refresh**: Dashboard updates every 10 seconds
- **RESTful API Integration**: Seamless backend communication

## Development

### Prerequisites

- Node.js 16+ and npm
- Backend server running on http://localhost:8080

### Install Dependencies

```bash
cd smart_home_web
npm install
```

### Run Development Server

```bash
npm start
```

App will open at http://localhost:3000

### Build for Production

```bash
npm run build
```

Production build will be in the `build/` directory.

## API Configuration

The app uses a proxy in development (configured in package.json):
```json
"proxy": "http://localhost:8080"
```

For production, set the `REACT_APP_API_URL` environment variable:
```bash
REACT_APP_API_URL=http://your-backend-url npm run build
```

## Project Structure

```
smart_home_web/
├── public/              # Static assets
│   └── index.html
├── src/
│   ├── components/      # Reusable components
│   │   ├── DeviceCard.js
│   │   └── AutomationCard.js
│   ├── pages/           # Page components
│   │   ├── Dashboard.js
│   │   └── Automations.js
│   ├── services/        # API services
│   │   └── api.js
│   ├── styles/          # CSS files
│   ├── App.js           # Main app component
│   └── index.js         # Entry point
└── package.json
```

## Deployment with Docker

The web app can be served via Nginx in production. See `docker-compose.yml` and `nginx.conf` for configuration.

## Technologies

- React 18.2
- React Router 6
- Axios for HTTP requests
- Recharts for data visualization
- date-fns for date formatting
