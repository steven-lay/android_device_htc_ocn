#
# Copyright (C) 2018 The LineageOS Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Audio
PRODUCT_PROPERTY_OVERRIDES += \
    af.fast_track_multiplier=1 \
    audio.deep_buffer.media=true \
    audio.heap.size.multiplier=7 \
    audio.offload.pcm.16bit.enable=true \
    audio.offload.pcm.24bit.enable=true \
    audio.offload.pstimeout.secs=3 \
    audio.offload.video=true \
    persist.vendor.audio.fluence.speaker=true \
    persist.vendor.audio.fluence.voicecall=true \
    persist.vendor.audio.fluence.voicerec=false \
    persist.vendor.audio.headset.anc.type=feedback \
    persist.vendor.audio.ras.enabled=false \
    persist.vendor.bt.a2dp_offload_cap=sbc-aptx \
    ro.config.media_vol_steps=25 \
    ro.vendor.audio.sdk.fluencetype=none \
    ro.vendor.audio.sdk.ssr=false \
    vendor.audio.dolby.ds2.enabled=false \
    vendor.audio.dolby.ds2.hardbypass=false \
    vendor.audio.flac.sw.decoder.24bit=true \
    vendor.audio.hw.aac.encoder=true \
    vendor.audio.noisy.broadcast.delay=600 \
    vendor.audio.offload.buffer.size.kb=1024 \
    vendor.audio.offload.multiaac.enable=true \
    vendor.audio.offload.multiple.enabled=true \
    vendor.audio.offload.passthrough=false \
    vendor.audio.offload.track.enable=true \
    vendor.audio.parser.ip.buffer.size=262144 \
    vendor.audio.safx.pbe.enabled=true \
    vendor.audio.tunnel.encode=false \
    vendor.audio.use.sw.alac.decoder=true \
    vendor.audio.use.sw.ape.decoder=true \
    vendor.audio_hal.period_size=192 \
    vendor.fm.a2dp.conc.disabled=true \
    vendor.voice.path.for.pcm.voip=true

# Bluetooth
PRODUCT_PROPERTY_OVERRIDES += \
    bt.max.hfpclient.connections=1 \
    qcom.bluetooth.soc=cherokee \
    ro.bluetooth.a4wp=false \
    ro.bluetooth.dun=true \
    ro.bluetooth.emb_wp_mode=true \
    ro.bluetooth.hfp.ver=1.7 \
    ro.bluetooth.remote.autoconnect=true \
    ro.bluetooth.request.master=true \
    ro.bluetooth.sap=true \
    ro.bluetooth.wipower=false \
    ro.bt.bdaddr_path=/sys/module/htc_bdaddress/parameters/bdaddress \
    ro.bt.ble_tag=1 \
    ro.bt.chipset=Qualcomm WCN3990 \
    ro.bt.profiles=12806147 \
    ro.bt.stack=Bluedroid \
    ro.bt.version=5.0 \
    ro.product.bluetooth=5.0 \
    ro.qc.bluetooth.stack=bluedroid

# Camera
PRODUCT_PROPERTY_OVERRIDES += \
    persist.camera.tuning_spec=htc \
    persist.ts.rtmakeup=false \
    vidc.enc.dcvs.extra-buff-count=2

# CNE
PRODUCT_PROPERTY_OVERRIDES += \
    persist.cne.feature=1

# Data modules
PRODUCT_PROPERTY_OVERRIDES += \
    ro.use_data_netmgrd=true \
    persist.data.netmgrd.qos.enable=true \
    persist.data.mode=concurrent

# Display
PRODUCT_PROPERTY_OVERRIDES += \
    debug.gralloc.enable_fb_ubwc=1 \
    debug.sf.enable_hwc_vds=1 \
    debug.sf.hw=1 \
    debug.sf.latch_unsignaled=1 \
    dev.pm.dyn_samplingrate=1 \
    persist.demo.hdmirotationlock=false \
    ro.opengles.version=196610 \
    ro.sf.lcd_density=640 \
    sdm.debug.disable_skip_validate=1

# Factory Reset Protection
PRODUCT_PROPERTY_OVERRIDES += \
    ro.frp.pst=/dev/block/bootdevice/by-name/frp

# GPS
PRODUCT_PROPERTY_OVERRIDES += \
    persist.igps.sensor=on

# IMS
PRODUCT_PROPERTY_OVERRIDES += \
    persist.ims.disableADBLogs=1 \
    persist.ims.disableDebugLogs=1 \
    persist.ims.disableIMSLogs=1 \
    persist.ims.disableQXDMLogs=1

# LineageOTA
PRODUCT_PROPERTY_OVERRIDES += \
    lineage.updater.uri=http://lineageota.ddns.net/api/v1/{device}/{type}/{incr}

# Media
PRODUCT_PROPERTY_OVERRIDES += \
    media.aac_51_output_enabled=true \
    media.stagefright.enable-aac=true \
    media.stagefright.enable-http=true \
    media.stagefright.enable-player=true \
    media.stagefright.enable-qcp=true \
    media.stagefright.enable-scan=true \
    mm.enable.qcom_parser=13631487 \
    mm.enable.smoothstreaming=true \
    mmp.enable.3g2=true \
    persist.media.treble_omx=false \
    persist.mm.enable.prefetch=true

# Perf
PRODUCT_PROPERTY_OVERRIDES += \
    ro.core_ctl_min_cpu=2 \
    ro.core_ctl_max_cpu=4 \
    ro.sys.fw.bg_apps_limit=60 \
    ro.vendor.extension_library=libqti-perfd-client.so \
    ro.vendor.qti.config.zram=true \
    sdm.perf_hint_window=50

# Radio
PRODUCT_PROPERTY_OVERRIDES += \
    DEVICE_PROVISIONED=1 \
    persist.radio.apm_sim_not_pwdn=0 \
    persist.vendor.radio.apm_mdm_not_pwdn=1 \
    persist.vendor.radio.cs_srv_type=1 \
    persist.vendor.radio.data_ltd_sys_ind=1 \
    persist.vendor.radio.fill_eons=1 \
    persist.vendor.radio.snapshot_timer=0 \
    rild.libpath=/vendor/lib64/libril-qc-qmi-1.so \
    ro.baseband.arch=msm

# RmNet Data
PRODUCT_PROPERTY_OVERRIDES += \
    persist.data.df.agg.dl_pkt=10 \
    persist.data.df.agg.dl_size=4096 \
    persist.data.df.dev_name=rmnet_usb0 \
    persist.data.df.dl_mode=5 \
    persist.data.df.iwlan_mux=9 \
    persist.data.df.mux_count=8 \
    persist.data.df.ul_mode=5 \
    persist.data.wda.enable=true \
    persist.rmnet.data.enable=true

# Sensors
PRODUCT_PROPERTY_OVERRIDES += \
    ro.qti.sensors.dev_ori=true \
    ro.qti.sensors.dpc=true \
    ro.qti.sensors.iod=true \
    ro.qti.sensors.mot_detect=true \
    ro.qti.sensors.multishake=true \
    ro.qti.sensors.pmd=true \
    ro.qti.sensors.sta_detect=true

# SSC/SLPI sensor daemon
PRODUCT_PROPERTY_OVERRIDES += \
    ro.qualcomm.slpi=true

# sdcardfs
PRODUCT_PROPERTY_OVERRIDES += \
    ro.sys.sdcardfs=true

# TCP
PRODUCT_PROPERTY_OVERRIDES += \
    net.tcp.buffersize.wifi=24288,2097152,4194304,262144,524288,1048576 \
    net.tcp.buffersize.default=4096,87380,524288,4096,16384,110208 \
    net.tcp.buffersize.lte=2097152,4194304,8388608,262144,524288,1048576 \
    net.tcp.buffersize.umts=4094,87380,110208,4096,16384,110208 \
    net.tcp.buffersize.hspa=4094,87380,1220608,4096,16384,1220608 \
    net.tcp.buffersize.hsupa=4094,87380,1220608,4096,16384,1220608 \
    net.tcp.buffersize.hsdpa=4094,87380,1220608,4096,16384,1220608 \
    net.tcp.buffersize.hspap=4094,87380,1220608,4096,16384,1220608 \
    net.tcp.buffersize.edge=4093,26280,35040,4096,16384,35040 \
    net.tcp.buffersize.gprs=4092,8760,11680,4096,8760,11680 \
    net.tcp.buffersize.evdo=4094,87380,524288,4096,16384,262144 \
    net.tcp.2g_init_rwnd=10

# Telephony
PRODUCT_PROPERTY_OVERRIDES += \
    ro.telephony.call_ring.multiple=false

# Time-services
PRODUCT_PROPERTY_OVERRIDES += \
    persist.backup.ntpServer="0.pool.ntp.org" \
    persist.delta_time.enable=true \
    persist.timed.enable=true

# VoLTE
PRODUCT_PROPERTY_OVERRIDES += \
    persist.dbg.ims_volte_enable=1 \
    persist.dbg.volte_avail_ovr=1 \
    persist.dbg.vt_avail_ovr=1 \
    persist.dbg.wfc_avail_ovr=1 \
    persist.volte_enabled_by_hw=1

# Wifi
PRODUCT_PROPERTY_OVERRIDES += \
    wifi.interface=wlan0
