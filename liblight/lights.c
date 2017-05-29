/*
 * Copyright (C) 2016 The CyanogenMod Project
 * Copyright (C) 2017 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "lights"

#include <cutils/log.h>

#include <errno.h>
#include <fcntl.h>
#include <pthread.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <sys/ioctl.h>
#include <sys/types.h>

#include <hardware/lights.h>

#define UNUSED __attribute__((unused))

#define MODE_OFF 0x0
#define MODE_ON 0x1
#define MODE_BLINK 0x2

#define MODE_SHIFT 24
#define MODE_MASK 0x0f000000

static pthread_once_t g_init = PTHREAD_ONCE_INIT;
static pthread_mutex_t g_lock = PTHREAD_MUTEX_INITIALIZER;

static struct light_state_t g_notification;
static struct light_state_t g_attention;
static struct light_state_t g_battery;

static bool write_led_error = false;

const char *const INDICATOR_LED_FILE = "/sys/class/leds/indicator/mode_and_lut_params";
const char *const BUTTON_BACKLIGHT_FILE = "/sys/class/leds/button-backlight/brightness";
const char *const LCD_BACKLIGHT_FILE = "/sys/class/leds/lcd-backlight/brightness";

static int write_file_string(const char *path, const char *value) {
    int fd;
    int rc = 0;
    int bytes, written;
    fd = open(path, O_RDWR);
    if (fd < 0) {
        rc = -errno;
        if (!write_led_error) {
            ALOGE("%s: failed to open %s\n", __func__, path);
            write_led_error = true;
        }
        return rc;
    }

    written = write(fd, value, strlen(value));
    if (written < 0) {
        rc = -errno;
        ALOGE("%s: failed to write %s\n", __func__, value);
    }
    close(fd);
    return rc;
}

static int write_file(const char *path, const char *format, uint32_t value) {
    char buffer[20];

    snprintf(buffer, sizeof(buffer), format, value);
    return write_file_string(path, buffer);
}

static int write_led(uint32_t mode_rgb, uint32_t on_ms, uint32_t off_ms)
{
    char buf[100];

    /* Scale this down because we can only flash quickly */
    sprintf(buf, "%x %u %u 0", mode_rgb, on_ms / 4, off_ms / 4);
    return write_file_string(INDICATOR_LED_FILE, buf);
}

static int write_button(uint32_t value) {
    /* button backlight expects a decimal number */
    return write_file(BUTTON_BACKLIGHT_FILE, "%d", value);
}

static int write_backlight(uint32_t value) {
    /* backlight expects a decimal number */
    return write_file(LCD_BACKLIGHT_FILE, "%d", value);
}

static uint32_t rgb_to_brightness(const struct light_state_t *state) {
    uint32_t color = state->color & 0x00ffffff;
    return ((77 * ((color >> 16) & 0xff)) + (150 * ((color >> 8) & 0xff)) +
            (29 * (color & 0xff))) >> 8;
}

static bool is_lit(const struct light_state_t *state) {
    return (state->color & 0x00ffffff);
}

void init_globals(void) {
    pthread_mutex_init(&g_lock, NULL);
}

static void set_speaker_light_locked(UNUSED struct light_device_t *dev,
        struct light_state_t *state) {
    int rc;
    uint32_t color;
    uint32_t indicator_value = 0x00000000;
    uint32_t flashOnMS = 0, flashOffMS = 0;

    if (state == NULL) {
        /* Unset everything */
        goto write_indicator_value;
    }

    flashOnMS = state->flashOnMS;
    flashOffMS = state->flashOffMS;
    color = state->color & 0x00ffffff;

    ALOGV("%s: color: 0x%08x", __func__, color);

    /* Set color */
    if ((color & 0x00ff0000) && (color & 0x0000ff00)) {
        /* amber */
        indicator_value |= 0x00ffff00;
    } else if (color & 0x00ff0000) {
        /* red */
        indicator_value |= 0x00ff0000;
    } else if (color & 0x0000ff00) {
        /* green */
        indicator_value |= 0x0000ff00;
    } else if (color & 0x000000ff) {
        /* LED not capable of blue, use green */
        indicator_value |= 0x0000ff00;
    }

    /* Set blink */
    if (is_lit(state) && state->flashMode == LIGHT_FLASH_TIMED) {
        indicator_value |= (MODE_BLINK << MODE_SHIFT);
    } else if (is_lit(state) && state->flashMode == LIGHT_FLASH_NONE) {
        indicator_value |= (MODE_ON << MODE_SHIFT);
    } else if (!is_lit(state)) {
        indicator_value &= (~MODE_MASK);
    }

write_indicator_value:
    rc = write_led(indicator_value, flashOnMS, flashOffMS);
    if (rc < 0) {
        if (!write_led_error) {
            ALOGE("%s: Error writing to LED file\n", __func__);
        }
    }
}

static void handle_speaker_battery_locked(struct light_device_t *dev) {
    if (is_lit(&g_notification)) {
        set_speaker_light_locked(dev, &g_notification);
    } else if (is_lit(&g_attention)) {
        set_speaker_light_locked(dev, &g_attention);
    } else if (is_lit(&g_battery)) {
        set_speaker_light_locked(dev, &g_battery);
    } else {
        /* No lights or blink */
        set_speaker_light_locked(dev, NULL);
    }
}

static int set_light_buttons(UNUSED struct light_device_t *dev,
        const struct light_state_t *state) {
    int rc = 0;
    uint32_t brightness = rgb_to_brightness(state);

    ALOGV("%s: brightness: 0x%08x", __func__, brightness);

    pthread_mutex_lock(&g_lock);

    rc = write_button(brightness);
    if (rc < 0) {
        if (!write_led_error) {
            ALOGE("%s: Failed to set button brightness to 0x%08x\n",
                    __func__, brightness);
        }
    }
    pthread_mutex_unlock(&g_lock);

    return rc;
}

static int set_light_backlight(UNUSED struct light_device_t *dev,
        const struct light_state_t *state) {
    int rc = 0;
    uint32_t brightness = rgb_to_brightness(state);

    ALOGV("%s: brightness: 0x%08x", __func__, brightness);

    pthread_mutex_lock(&g_lock);

    rc = write_backlight(brightness);
    if (rc < 0) {
        if (!write_led_error) {
            ALOGE("%s: Failed to set backlight brightness to 0x%08x\n",
                    __func__, brightness);
        }
    }
    pthread_mutex_unlock(&g_lock);

    return rc;
}

static int set_light_attention(struct light_device_t *dev,
        const struct light_state_t *state) {
    pthread_mutex_lock(&g_lock);

    g_attention = *state;
    handle_speaker_battery_locked(dev);

    pthread_mutex_unlock(&g_lock);

    return 0;
}

static int set_light_notifications(struct light_device_t *dev,
        const struct light_state_t *state) {
    pthread_mutex_lock(&g_lock);

    g_notification = *state;
    handle_speaker_battery_locked(dev);

    pthread_mutex_unlock(&g_lock);

    return 0;
}

static int set_light_battery(struct light_device_t *dev,
        const struct light_state_t *state) {
    pthread_mutex_lock(&g_lock);

    g_battery = *state;
    handle_speaker_battery_locked(dev);

    pthread_mutex_unlock(&g_lock);

    return 0;
}

static int close_lights(struct light_device_t *dev) {
    if (dev) {
        free(dev);
    }
    return 0;
}

static int open_lights(const struct hw_module_t *module, const char *name,
        struct hw_device_t **device) {
    int (*set_light)(struct light_device_t *dev,
            const struct light_state_t *state);
    struct light_device_t *dev;

    if (0 == strcmp(LIGHT_ID_BACKLIGHT, name)) {
        set_light = set_light_backlight;
    } else if (0 == strcmp(LIGHT_ID_BUTTONS, name)) {
        set_light = set_light_buttons;
    } else if (0 == strcmp(LIGHT_ID_ATTENTION, name)) {
        set_light = set_light_attention;
    } else if (0 == strcmp(LIGHT_ID_NOTIFICATIONS, name))  {
        set_light = set_light_notifications;
    } else if (0 == strcmp(LIGHT_ID_BATTERY, name)) {
        set_light = set_light_battery;
    } else {
        return -EINVAL;
    }

    pthread_once(&g_init, init_globals);
    dev = malloc(sizeof(struct light_device_t));
    memset(dev, 0, sizeof(struct light_device_t));

    dev->common.tag = HARDWARE_DEVICE_TAG;
    dev->common.version = 0;
    dev->common.module = (struct hw_module_t *) module;
    dev->common.close = (int (*)(struct hw_device_t *)) close_lights;
    dev->set_light = set_light;

    *device = (struct hw_device_t *) dev;
    return 0;

}

static struct hw_module_methods_t lights_module_methods = {
    .open = open_lights,
};

struct hw_module_t HAL_MODULE_INFO_SYM = {
    .tag = HARDWARE_MODULE_TAG,
    .version_major = 1,
    .version_minor = 0,
    .id = LIGHTS_HARDWARE_MODULE_ID,
    .name = "Lights module",
    .author = "The LineageOS Project",
    .methods = &lights_module_methods,
};
