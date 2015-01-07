#!/usr/bin/env bash
export DEBIAN_FRONTEND=noninteractive

usermod -a -G audio vagrant
mkdir /mpd
mkdir /mpd/playlists
touch /mpd/database
touch /mpd/pid
touch /mpd/state
touch /mpd/sticker.sql
apt-get update
apt-get install -y mpd
killall mpd
cp /vagrant/resources/vagrant/mpd.conf /etc/mpd.conf
service mpd restart
