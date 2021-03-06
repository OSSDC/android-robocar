DISK = /dev/disk2

flash:
	# The compressed image file expands to over 4GB. This can cause problems
	# for the built-in tools on some platforms. If you are unable to unzip
	# the archive, or see a message stating that it's corrupt, use 7-Zip
	# (Windows) or The Unarchiver (Mac OS) instead.
	diskutil unmountDisk $(DISK)
	sudo dd bs=1m if=iot_rpi3.img of=$(DISK)

connect:
	adb connect Android.local
