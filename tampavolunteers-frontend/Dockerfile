# Development Dockerfile for Vite frontend
FROM node:20-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy source code
COPY . .

# Expose Vite port
EXPOSE 5173

# Run in development mode with host flag to allow external connections
CMD ["npm", "run", "dev", "--", "--host"]
