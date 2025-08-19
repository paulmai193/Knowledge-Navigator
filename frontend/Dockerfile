FROM node:18-alpine

WORKDIR /app

# Copy package files
COPY package.json yarn.lock* package-lock.json* ./

# Install dependencies
RUN npm install

# Install serve globally
RUN npm install -g serve

# Copy source code
COPY . .

# Build the application
RUN npm run build

# Serve the application
EXPOSE 3000
CMD ["serve", "-s", "build", "-l", "3000"]