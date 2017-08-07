#
# Copyright 2016 The CyanogenMod Project
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

# This contains the module build definitions for the hardware-specific
# components for this device.
#
# As much as possible, those components should be built unconditionally,
# with device-specific names to avoid collisions, to avoid device-specific
# bitrot and build breakages. Building a component unconditionally does
# *not* include it on all devices, so it is safe even with hardware-specific
# components.

LOCAL_PATH := $(call my-dir)

ifneq ($(filter ocn,$(TARGET_DEVICE)),)

include $(call all-makefiles-under,$(LOCAL_PATH))

include $(CLEAR_VARS)

KEYMASTER_IMAGES := \
    keymaste.b00 keymaste.b01 keymaste.b02 keymaste.b03 keymaste.b04 keymaste.b05 \
    keymaste.b06 keymaste.b07 keymaste.mdt

KEYMASTER_SYMLINKS := $(addprefix $(TARGET_ROOT_OUT)/firmware/image/,$(notdir $(KEYMASTER_IMAGES)))
$(KEYMASTER_SYMLINKS): $(LOCAL_INSTALLED_MODULE)
	@echo "Keymaster firmware link: $@"
	@mkdir -p $(dir $@)
	@rm -rf $@
	$(hide) ln -sf /etc/firmware/$(notdir $@) $@

ALL_DEFAULT_INSTALLED_MODULES += $(KEYMASTER_SYMLINKS)

MODEM_IMAGES := \
    mba.mbn qdsp6m.qdb modem.b00 modem.b01 modem.b02 modem.b03 modem.b04 modem.b05 \
    modem.b06 modem.b07 modem.b08 modem.b09 modem.b10 modem.b11 modem.b12 modem.b13 \
    modem.b14 modem.b15 modem.b16 modem.b17 modem.b18 modem.b19 modem.b20 modem.b21 \
    modem.b22 modem.b23 modem.b24 modem.b25 modem.b26 modem.b27 modem.b28 modem.mdt

MODEM_SYMLINKS := $(addprefix $(TARGET_ROOT_OUT)/firmware/image/,$(notdir $(MODEM_IMAGES)))
$(MODEM_SYMLINKS): $(LOCAL_INSTALLED_MODULE)
	@echo "Modem firmware link: $@"
	@mkdir -p $(dir $@)
	@rm -rf $@
	$(hide) ln -sf /firmware/radio/$(notdir $@) $@

ALL_DEFAULT_INSTALLED_MODULES += $(MODEM_SYMLINKS)

ADSP_IMAGES := \
    adsp.b00 adsp.b01 adsp.b02 adsp.b03 adsp.b04 adsp.b05 adsp.b06 adsp.b08 \
    adsp.b09 adsp.b11 adsp.mdt

ADSP_SYMLINKS := $(addprefix $(TARGET_ROOT_OUT)/firmware/image/,$(notdir $(ADSP_IMAGES)))
$(ADSP_SYMLINKS): $(LOCAL_INSTALLED_MODULE)
	@echo "ADSP firmware link: $@"
	@mkdir -p $(dir $@)
	@rm -rf $@
	$(hide) ln -sf /firmware/adsp/$(notdir $@) $@

ALL_DEFAULT_INSTALLED_MODULES += $(ADSP_SYMLINKS)

CPE_IMAGES := \
    cpe_9340.b00 cpe_9340.b01 cpe_9340.b02 cpe_9340.b03 cpe_9340.b04 cpe_9340.b05 \
    cpe_9340.b06 cpe_9340.b07 cpe_9340.b08 cpe_9340.b09 cpe_9340.b10 cpe_9340.b11 \
    cpe_9340.b12 cpe_9340.b13 cpe_9340.b14 cpe_9340.b15 cpe_9340.b16 cpe_9340.b17 \
    cpe_9340.b18 cpe_9340.b19 cpe_9340.b20 cpe_9340.b21 cpe_9340.b22 cpe_9340.mdt

CPE_SYMLINKS := $(addprefix $(TARGET_ROOT_OUT)/firmware/image/,$(notdir $(CPE_IMAGES)))
$(CPE_SYMLINKS): $(LOCAL_INSTALLED_MODULE)
	@echo "CPE firmware link: $@"
	@mkdir -p $(dir $@)
	@rm -rf $@
	$(hide) ln -sf /firmware/adsp/$(notdir $@) $@

ALL_DEFAULT_INSTALLED_MODULES += $(CPE_SYMLINKS)

SLPI_V1_IMAGES := \
    spli_v1.b00 spli_v1.b01 spli_v1.b02 spli_v1.b03 spli_v1.b04 spli_v1.b05 spli_v1.b06 \
    spli_v1.b07 spli_v1.b08 spli_v1.b09 spli_v1.b10 spli_v1.b11 spli_v1.b12 spli_v1.b13 \
    spli_v1.b14 spli_v1.b15 spli_v1.mdt slpi_v1.cfg

SLPI_V1_SYMLINKS := $(addprefix $(TARGET_ROOT_OUT)/firmware/image/,$(notdir $(SLPI_V1_IMAGES)))
$(SLPI_V1_SYMLINKS): $(LOCAL_INSTALLED_MODULE)
	@echo "SLPI_V1 firmware link: $@"
	@mkdir -p $(dir $@)
	@rm -rf $@
	$(hide) ln -sf /firmware/spli_v1.$(notdir $@) $@

ALL_DEFAULT_INSTALLED_MODULES += $(SLPI_V1_SYMLINKS)

SLPI_V2_IMAGES := \
    spli_v2.b00 spli_v2.b01 spli_v2.b02 spli_v2.b03 spli_v2.b04 spli_v2.b05 spli_v2.b06 \
    spli_v2.b07 spli_v2.b08 spli_v2.b09 spli_v2.b10 spli_v2.b11 spli_v2.b12 spli_v2.b13 \
    spli_v2.b14 spli_v2.b15 spli_v2.mdt slpi_v2.cfg

SLPI_V2_SYMLINKS := $(addprefix $(TARGET_ROOT_OUT)/firmware/image/,$(notdir $(SLPI_V2_IMAGES)))
$(SLPI_V2_SYMLINKS): $(LOCAL_INSTALLED_MODULE)
	@echo "SLPI_V2 firmware link: $@"
	@mkdir -p $(dir $@)
	@rm -rf $@
	$(hide) ln -sf /firmware/spli_v2.$(notdir $@) $@

ALL_DEFAULT_INSTALLED_MODULES += $(SLPI_V2_SYMLINKS)

VENUS_IMAGES := \
    venus.b00 venus.b01 venus.b02 venus.b03 venus.b04 venus.mdt

VENUS_SYMLINKS := $(addprefix $(TARGET_ROOT_OUT)/firmware/image/,$(notdir $(VENUS_IMAGES)))
$(VENUS_SYMLINKS): $(LOCAL_INSTALLED_MODULE)
	@echo "VENUS firmware link: $@"
	@mkdir -p $(dir $@)
	@rm -rf $@
	$(hide) ln -sf /firmware/venus/$(notdir $@) $@

ALL_DEFAULT_INSTALLED_MODULES += $(VENUS_SYMLINKS)

WIDEVINE_IMAGES := \
    windevine.b00 windevine.b01 windevine.b02 windevine.b03 windevine.b04 \
    windevine.b05 windevine.b06 windevine.b07 windevine.mdt

WIDEVINE_SYMLINKS := $(addprefix $(TARGET_ROOT_OUT)/firmware/image/,$(notdir $(WIDEVINE_IMAGES)))
$(WIDEVINE_SYMLINKS): $(LOCAL_INSTALLED_MODULE)
	@echo "WIDEVINE firmware link: $@"
	@mkdir -p $(dir $@)
	@rm -rf $@
	$(hide) ln -sf /system/etc/firmware/$(notdir $@) $@

ALL_DEFAULT_INSTALLED_MODULES += $(WIDEVINE_SYMLINKS)

IMS_LIBS := libimscamera_jni.so libimsmedia_jni.so
IMS_SYMLINKS := $(addprefix $(TARGET_OUT_VENDOR_APPS)/ims/lib/arm64/,$(notdir $(IMS_LIBS)))
$(IMS_SYMLINKS): $(LOCAL_INSTALLED_MODULE)
	@echo "IMS lib link: $@"
	@mkdir -p $(dir $@)
	@rm -rf $@
	$(hide) ln -sf /system/vendor/lib64/$(notdir $@) $@

ALL_DEFAULT_INSTALLED_MODULES += $(IMS_SYMLINKS)

RFS_MSM_ADSP_SYMLINKS := $(TARGET_OUT)/rfs/msm/adsp/
$(RFS_MSM_ADSP_SYMLINKS): $(LOCAL_INSTALLED_MODULE)
	@echo "Creating RFS MSM ADSP folder structure: $@"
	@rm -rf $@/*
	@mkdir -p $(dir $@)/readonly
	$(hide) ln -sf /data/tombstones/lpass $@/ramdumps
	$(hide) ln -sf /persist/rfs/msm/adsp $@/readwrite
	$(hide) ln -sf /persist/rfs/shared $@/shared
	$(hide) ln -sf /persist/hlos_rfs/shared $@/hlos
	$(hide) ln -sf /firmware $@/readonly/firmware
	$(hide) ln -sf /vendor/firmware $@/readonly/vendor

RFS_MSM_MPSS_SYMLINKS := $(TARGET_OUT)/rfs/msm/mpss/
$(RFS_MSM_MPSS_SYMLINKS): $(LOCAL_INSTALLED_MODULE)
	@echo "Creating RFS MSM MPSS folder structure: $@"
	@rm -rf $@/*
	@mkdir -p $(dir $@)/readonly
	$(hide) ln -sf /data/tombstones/modem $@/ramdumps
	$(hide) ln -sf /persist/rfs/msm/mpss $@/readwrite
	$(hide) ln -sf /persist/rfs/shared $@/shared
	$(hide) ln -sf /persist/hlos_rfs/shared $@/hlos
	$(hide) ln -sf /firmware $@/readonly/firmware
	$(hide) ln -sf /vendor/firmware $@/readonly/vendor

ALL_DEFAULT_INSTALLED_MODULES += $(RFS_MSM_ADSP_SYMLINKS) $(RFS_MSM_MPSS_SYMLINKS)

WCNSS_INI_SYMLINK := $(TARGET_OUT_ETC)/firmware/wlan/qca_cld/WCNSS_qcom_cfg.ini
$(WCNSS_INI_SYMLINK): $(LOCAL_INSTALLED_MODULE)
	@echo "WCNSS config ini link: $@"
	@mkdir -p $(dir $@)
	@rm -rf $@
	$(hide) ln -sf /system/etc/wifi/$(notdir $@) $@

WCNSS_MAC_SYMLINK := $(TARGET_OUT_ETC)/firmware/wlan/qca_cld/wlan_mac.bin
$(WCNSS_MAC_SYMLINK): $(LOCAL_INSTALLED_MODULE)
	@echo "WCNSS MAC bin link: $@"
	@mkdir -p $(dir $@)
	@rm -rf $@
	$(hide) ln -sf /persist/$(notdir $@) $@

ALL_DEFAULT_INSTALLED_MODULES += $(WCNSS_INI_SYMLINK) $(WCNSS_MAC_SYMLINK)

WCD_IMAGES := \
    wcd9320_anc.bin wcd9320_mad_audio.bin wcd9320_mbhc.bin

WCD_SYMLINKS := $(addprefix $(TARGET_OUT_ETC)/firmware/wcd9320/,$(notdir $(WCD_IMAGES)))
$(WCD_SYMLINKS): $(LOCAL_INSTALLED_MODULE)
	@echo "WCD9320 firmware link: $@"
	@mkdir -p $(dir $@)
	@rm -rf $@
	$(hide) ln -sf /data/misc/audio/$(notdir $@) $@

ALL_DEFAULT_INSTALLED_MODULES += $(WCD_SYMLINKS)

endif
