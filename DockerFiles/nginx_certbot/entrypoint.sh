#!/bin/sh

set -e

# Var checks
if [ -n "$APPSERVER_HOST" ] && \
    [ -n "$APPSERVER_PORT" ] && \
    [ -n "$DOMAIN" ] && \
    [ -n "$LE_EMAIL" ]; then
    # Inject variables
    sed -i s/__APPSERVER_HOST__/$APPSERVER_HOST/g /etc/nginx/conf.d/appserver.conf
    sed -i s/__APPSERVER_PORT__/$APPSERVER_PORT/g /etc/nginx/conf.d/appserver.conf

    sed -i s/__DOMAIN__/$DOMAIN/g /etc/nginx/conf.d/appserver.conf
    sed -i s/__DOMAIN__/$DOMAIN/g /etc/nginx/conf.d/le.conf

    # https://github.com/docker/compose/issues/2854 :(
    # See https://stackoverflow.com/a/24358387/1161743
    # Strip literal quotes in variable value
    LE_OPTIONS=$(eval echo $LE_OPTIONS)
    LE_RENEW_OPTIONS=$(eval echo $LE_RENEW_OPTIONS)
    LE_RENEW_CRON_COMMAND=$(eval echo $LE_RENEW_CRON_COMMAND)

    # Disable appserver config first as cert not present.
    mv -v /etc/nginx/conf.d/appserver.conf /etc/nginx/conf.d/appserver.conf.disabled

    (
        # Give nginx time to start
        sleep 5

        echo "Starting Let's Encrypt certificate install..."
        certbot certonly -n "${LE_OPTIONS}" \
            --agree-tos --email "${LE_EMAIL}" \
            --webroot -w /usr/share/nginx/html -d $DOMAIN

        # Enable appserver config
        mv -v /etc/nginx/conf.d/appserver.conf.disabled /etc/nginx/conf.d/appserver.conf

        echo "Reloading NGINX with SSL..."
        nginx -s reload

        # Install crontab for cert renewal
        touch crontab.tmp \
            && echo "37 2 * * * certbot renew ${LE_RENEW_OPTIONS} --post-hook 'nginx -s reload' && ${LE_RENEW_CRON_COMMAND} > /dev/null 2>&1" > crontab.tmp \
            && crontab crontab.tmp \
            && rm crontab.tmp

        # Start crond
        /usr/sbin/crond
    ) &

    # Start nginx
    echo "Starting NGINX..."
    nginx -g "daemon off;"
else
    echo "ERROR: please provide APPSERVER_HOST, APPSERVER_PORT, DOMAIN, LE_EMAIL"
fi

