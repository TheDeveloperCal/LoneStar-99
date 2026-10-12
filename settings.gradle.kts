// Copyright (C) 2026 DeveloperCal
// SPDX-License-Identifier: GPL-3.0-only

pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "TI-99 Pocket"
include(":app")
