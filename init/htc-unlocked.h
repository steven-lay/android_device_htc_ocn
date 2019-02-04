/* Gen_Unlock_1.11.617.1: BS_US001 */
static bool is_variant_unlocked(std::string bootcid) {
    if (bootcid == "BS_US001") return true;
    return false;
}

static const char *htc_unlocked_properties =
    "ro.build.product=htc_ocnwhl\n"
    "ro.product.device=htc_ocnwhl\n"
    "ro.product.model=HTC u11\n"
    "persist.radio.NETWORK_SWITCH=2\n"
    "ro.telephony.default_network=9\n"
;
