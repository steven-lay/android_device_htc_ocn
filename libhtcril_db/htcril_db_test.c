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

#include <stdio.h>

int htcril_db_property_get(const char *name, char *value_ret, char *def_value);
int htcril_db_property_set(const char *name, const char *value);

int main(int argc, char **argv) {
    char value[400];

    if (argc == 1 || argc > 3) {
        fprintf(stderr, "usage: name [value]\n");
        return(1);
    } else if (argc == 2) {
        if (htcril_db_property_get(argv[1], value, "DEFAULT_VALUE") != 0) {
           fprintf(stderr, "failed to get the value %s\n", argv[1]);
           return(1);
        }
        printf("%s\n", value);
    } else {
        if (htcril_db_property_set(argv[1], argv[2]) != 0) {
            fprintf(stderr, "failed to set the value %s to %s\n", argv[1], argv[2]);
            return(1);
        }
    }
    return 0;
}
