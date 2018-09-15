#
# Copyright (C) 2017 The LineageOS Open Source Project
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

# Release name
PRODUCT_RELEASE_NAME := ocn

# Inherit some common Lineage stuff.
$(call inherit-product, vendor/lineage/config/common_full_phone.mk)

# Inherit device configuration
$(call inherit-product, device/htc/ocn/full_ocn.mk)

## Device identifier. This must come after all inclusions
PRODUCT_DEVICE := ocn
PRODUCT_NAME := lineage_ocn
PRODUCT_BRAND := HTC
PRODUCT_MODEL := HTC U11
PRODUCT_MANUFACTURER := HTC

PRODUCT_BUILD_PROP_OVERRIDES += \
        PRODUCT_NAME=ocnwhl_00617 \
        PRIVATE_BUILD_DESC="ocnwhl_00617-user 8.0.0 OOPR6.170623.013 1011554.6 release-keys"

BUILD_FINGERPRINT := htc/ocnwhl_00617/htc_ocnwhl:8.0.0/OPR6.170623.013/1011554.6:user/release-keys
