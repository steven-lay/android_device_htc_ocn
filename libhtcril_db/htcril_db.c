/*
 * Copyright (C) 2016 The CyanogenMod Project
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

#define LOG_TAG  "htcril_db"

#include <assert.h>
#include <string.h>
#include <pthread.h>
#include "../../../../external/sqlite/dist/sqlite3.h"

#include "cutils/log.h"

static pthread_mutex_t lock = PTHREAD_MUTEX_INITIALIZER;
static int done_init;
static sqlite3 *handle;

#define DB_FILENAME "/carrier/htcril.db"
#define TABLE_NAME "htcril_properties_table"

#define CREATE_TABLE_SQL \
    "CREATE TABLE IF NOT EXISTS " TABLE_NAME \
       "(property TEXT, value TEXT, PRIMARY KEY(property))"

#define UNUSED __attribute__ ((unused))

static int db_init_locked(void) {
    char *error_str = NULL;

    if (done_init) {
        return 0;
    }

    if (sqlite3_initialize()) {
        ALOGE("failed to initialize sqlite");
        return -1;
    }

    if (sqlite3_open(DB_FILENAME, &handle)) {
        ALOGE("failed to open %s", DB_FILENAME);
        sqlite3_shutdown();
        return -1;
    }

    ALOGV("execute: %s", CREATE_TABLE_SQL);
    if (sqlite3_exec(handle, CREATE_TABLE_SQL, NULL, NULL, &error_str)) {
        ALOGE("failed to exec sql [%s]: %s", CREATE_TABLE_SQL, error_str);
        sqlite3_free(error_str);
        sqlite3_close(handle);
        sqlite3_shutdown();
        return -1;
    }

    done_init = 1;

    return 0;
}

int htcril_db_init(void) {
    int ret;

    pthread_mutex_lock(&lock);
    ret = db_init_locked();
    pthread_mutex_unlock(&lock);

    return ret;
}

static int get_cb(void *result, int argc UNUSED, char **argv, char **column UNUSED) {
    strcpy(result, argv[0]);
    return 0;
}

static int db_property_get_locked(const char *name, char *value_ret, const char *def_value) {
    char sql[400];
    char *error_str = NULL;

    value_ret[0] = '\0';

    snprintf(sql, sizeof(sql), "SELECT value FROM " TABLE_NAME " WHERE property = '%s'", name);

    ALOGV("execute: %s", sql);
    if (sqlite3_exec(handle, sql, get_cb, value_ret, &error_str)) {
        ALOGE("failed to execute sql [%s]: %s", sql, error_str);
        sqlite3_free(error_str);
        return -1;
    }

    if (value_ret[0] == '\0') {
        strcpy(value_ret, def_value);
    }

    return 0;
}

int htcril_db_property_get(const char *name, char *value_ret, const char *def_value) {
    int ret;

    pthread_mutex_lock(&lock);
    if ((ret = db_init_locked()) == 0) {
        ret = db_property_get_locked(name, value_ret, def_value);
    }
    pthread_mutex_unlock(&lock);

    return ret;
}

static int db_property_set_locked(const char *name, const char *value) {
    char sql[400];
    char *error_str = NULL;

    snprintf(sql, sizeof(sql),
        "INSERT OR REPLACE INTO " TABLE_NAME "(property, value) VALUES ('%s', '%s')",
        name, value);

    ALOGV("execute: %s", sql);
    if (sqlite3_exec(handle, sql, NULL, NULL, &error_str)) {
        ALOGE("Failed to exec sql [%s]: %s", sql, error_str);
        sqlite3_free(error_str);
        return -1;
    }

    return 0;
}

int htcril_db_property_set(const char *name, const char *value) {
    int ret;

    pthread_mutex_lock(&lock);
    if ((ret = db_init_locked()) == 0) {
        ret = db_property_set_locked(name, value);
    }
    pthread_mutex_unlock(&lock);

    return ret;
}

static int db_reset_cleanup_locked(void) {
    int ret;

    if (!done_init) {
        return 0;
    }
    ret = sqlite3_close(handle);
    sqlite3_shutdown();
    return ret;
}

int htcril_db_reset_cleanup(void) {
    int ret;

    pthread_mutex_lock(&lock);
    ret = db_reset_cleanup_locked();
    pthread_mutex_unlock(&lock);

    return ret;
}
