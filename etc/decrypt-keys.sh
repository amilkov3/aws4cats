#!/bin/sh

openssl aes-256-cbc -K $encrypted_723854e8a597_key -iv $encrypted_723854e8a597_iv -in etc/travis-deploy-key.enc -out etc/travis-deploy-key -d;
chmod 600 etc/travis-deploy-key;
cp etc/travis-deploy-key ~/.ssh/id_rsa;
