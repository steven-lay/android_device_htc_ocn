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

endif
