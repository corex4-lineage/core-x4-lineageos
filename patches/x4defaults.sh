#!/system/bin/sh
# Core-X4 : reglages par defaut appliques une seule fois (1er demarrage)
[ "$(settings get global x4_defaults_done)" = "1" ] && exit 0
# Le HAL lumiere Crosscall relie le "voyant batterie" au flash photo -> la lampe s'allumait en charge.
content insert --uri content://lineagesettings/system --bind name:s:battery_light_enabled --bind value:s:0
settings put system notification_light_pulse 0
# Active le service X4 Boutons (2 boutons programmables)
SVC=fr.x4.boutons/fr.x4.boutons.KeyService
CUR=$(settings get secure enabled_accessibility_services)
case "$CUR" in *"$SVC"*) ;; null|"") settings put secure enabled_accessibility_services "$SVC";; *) settings put secure enabled_accessibility_services "$CUR:$SVC";; esac
settings put secure accessibility_enabled 1
settings put global device_name 'Core-X4'
settings put secure bluetooth_name 'Core-X4'
settings put global x4_defaults_done 1