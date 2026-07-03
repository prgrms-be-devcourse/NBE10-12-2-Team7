# 로컬 배포용 Next.js SSR 컨테이너 (nginx 뒤에서 :3000 서빙)
# next.config.ts 의 output:"standalone" 산출물(.next/standalone)만 실어 이미지를 가볍게.

# ---- 1) deps: 의존성 설치 (lockfile 기준) ----
FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# ---- 2) builder: next build ----
FROM node:22-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# ---- 3) runner: standalone 런타임만 ----
FROM node:22-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME=0.0.0.0
COPY --from=builder /app/public ./public
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static
EXPOSE 3000
CMD ["node", "server.js"]
