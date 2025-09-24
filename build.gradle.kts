// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    // Keep versions here so module plugins can omit versions
    id("com.android.application") version "8.12.3" apply false
    kotlin("android") version "1.9.25" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}
