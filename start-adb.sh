#!/bin/bash

# In case we have adb only on our user account on path and not on root's path
ADB=`which adb`
sudo $ADB kill-server
sudo $ADB start-server
