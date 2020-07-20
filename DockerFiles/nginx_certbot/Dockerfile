FROM nginx:stable-alpine

MAINTAINER Mikael Tollefsen <mikael@tollefsen.io>

# Nginx configuration
RUN rm -rf /etc/nginx/conf.d/*
COPY appserver.conf /etc/nginx/conf.d/appserver.conf
COPY le.conf /etc/nginx/conf.d/le.conf

# Install certbot
RUN apk update && \
  apk add --no-cache certbot curl

# SSL configuration for Let's Encrypt
RUN mkdir -p /etc/letsencrypt
VOLUME /etc/letsencrypt

# Copy entrypoint.sh
COPY entrypoint.sh /custom/entrypoint.sh

CMD ["/custom/entrypoint.sh"]