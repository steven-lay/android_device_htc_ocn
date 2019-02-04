// EE_UK | MID: 2PZC10000 | CID: EVE_001
static bool is_variant_eeuk(std::string bootcid) {
    if (bootcid == "EVE__001") return true;
    return false;
}

static const char *htc_eeuk_properties =
    "ro.product.model=HTC U11\n"
    "ro.ril.oem.ecclist=999,112,911\n"
    "ro.ril.enable.pre_r8fd=1\n"
    "ro.ril.disable.sync_pf=0\n"
    "ro.ril.hsxpa=5\n"
    "ro.ril.hsdpa.category=24\n"
    "ro.ril.hsupa.category=6\n"
    "ro.ril.disable.cpc=1\n"
    "ro.product.device=htc_ocnuhl\n"
    "ro.build.product=htc_ocnuhl\n"
    "persist.radio.VT_CAM_INTERFACE=2\n"
    "ro.telephony.default_network=9\n"
    "persist.radio.sap_silent_pin=1\n"
;
