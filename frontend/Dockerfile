FROM public.ecr.aws/docker/library/node:18-alpine

WORKDIR /app

# Copy package files
COPY package.json yarn.lock* package-lock.json* ./

# Clear npm cache and install dependencies
RUN npm cache clean --force
RUN rm -rf node_modules package-lock.json yarn.lock
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