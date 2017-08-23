#!/bin/sh
# loggy.sh

_date=`date +%F_%H-%M-%S`

case "$1" in
  system)
    logcat > /cache/loggy_system_${_date}.txt
    ;;
  *)
    cat /dev/kmsg > /cache/loggy_kernel_${_date}.txt
    ;;
esac

