# Copyright (C) 2017 The LineageOS Project
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

def FullOTA_InstallEnd(info):
  info.script.AppendExtra('if (getprop("ro.boot.mid") == "2PZC30000" || getprop("ro.boot.mid") == "2PZC40000") then')
  info.script.Print("This is a DS device - renaming radio props")
  info.script.AppendExtra('mount("ext4", "EMMC", "/dev/block/bootdevice/by-name/system", "/system", "");')
  info.script.AppendExtra('rename("/vendor/lib64/libril-qc-ltedirectdisc.so.dugl", "/vendor/lib64/libril-qc-ltedirectdisc.so");')
  info.script.AppendExtra('rename("/vendor/lib64/libril-qc-qmi-1.so.dugl", "/vendor/lib64/libril-qc-qmi-1.so");')
  info.script.AppendExtra('rename("/vendor/lib64/libril-qc-radioconfig.so.dugl", "/vendor/lib64/libril-qc-radioconfig.so");')
  info.script.AppendExtra('rename("/vendor/lib64/libril-qcril-hook-oem.so.dugl", "/vendor/lib64/libril-qcril-hook-oem.so");')
  info.script.AppendExtra('unmount("/system");')
  info.script.AppendExtra('endif;')
