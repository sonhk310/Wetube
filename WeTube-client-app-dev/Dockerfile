# ===== deps =====
FROM node:24-alpine AS deps
WORKDIR /app
COPY package*.json ./
RUN npm install && npm cache clean --force

# ===== build =====
FROM node:24-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .

# ✅ Nhận các biến từ build-args
ARG NEXT_PUBLIC_API_BASE
ARG NEXT_PUBLIC_LIVEKIT_URL
ARG NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME

# ✅ Ghi các biến này vào file .env.production
# Next.js sẽ tự động đọc file này khi build
RUN echo "NEXT_PUBLIC_API_BASE=${NEXT_PUBLIC_API_BASE}" > .env.production
RUN echo "NEXT_PUBLIC_LIVEKIT_URL=${NEXT_PUBLIC_LIVEKIT_URL}" >> .env.production
RUN echo "NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME=${NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME}" >> .env.production


RUN npm run build

# ===== run =====
FROM node:24-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public
ENV PORT=3000
EXPOSE 3000
CMD ["node", "server.js"]
