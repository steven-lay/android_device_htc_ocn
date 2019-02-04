// Sprint | 2PZC50000 | CID SPCS_001
static bool is_variant_sprint(std::string bootcid) {
    if (bootcid == "SPCS_001") return true;
    return false;
}

static const char *htc_sprint_properties =
    "ro.product.model=2PZC5\n"
    "ro.ril.oem.ecclist=911\n"
    "ro.ril.enable.pre_r8fd=1\n"
    "ro.ril.set.mtusize=1422\n"
    "ro.ril.hsxpa=2\n"
    "ro.ril.hsdpa.category=10\n"
    "ro.ril.hsupa.category=6\n"
    "ro.ril.def.agps.mode=6\n"
    "ro.ril.ignore_nw_mtu=1\n"
    "ro.product.device=htc_ocnwhl\n"
    "ro.build.product=htc_ocnwhl\n"
    "persist.radio.VT_CAM_INTERFACE=2\n"
    "ro.telephony.default_network=10\n"
;
