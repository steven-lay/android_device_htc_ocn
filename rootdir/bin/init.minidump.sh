#!/vendor/bin/sh

resetreason=`getprop ro.boot.resetreason`

dst_folder="/data/misc/radio/minidump"
src_file="/dev/block/bootdevice/by-name/ramdump"

rm -rf $dst_folder

if [ -e $src_file ] && [ "$resetreason" != "normal" ]; then
	mkdir $dst_folder
	chown radio.radio $dst_folder
	chmod 0700 $dst_folder
	cd $dst_folder
	parse_minidump $src_file > PARSE_LOG.BIN
	tar -czvf ../Exception_Dump .
	rm *.*
	mv ../Exception_Dump ./
	chown radio.radio "Exception_Dump"
	chmod 0400 "Exception_Dump"
fi
