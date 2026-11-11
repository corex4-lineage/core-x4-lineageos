#!/bin/bash
# Recette Core-X4 : applique les correctifs a une GSI LineageOS (TrebleDroid AndyYan, arm64 "b").
# Usage (Linux/WSL, e2fsprogs + python3) : ./patch_gsi.sh lineage-xx-arm64_bgN.img
set -e
I="$1"; cd "$(dirname "$0")"
printf 'u:object_r:system_file:s0\0' > c1.bin; printf 'u:object_r:rootfs:s0\0' > c3.bin
e2fsck -fy "$I" >/dev/null 2>&1 || true; resize2fs "$I" 4000M
put() { debugfs -w -R "rm $2" "$I" 2>/dev/null || true; debugfs -w -R "write $1 $2" "$I"; debugfs -w -R "sif $2 mode $3" "$I"; debugfs -w -R "ea_set -f c1.bin $2 security.selinux" "$I"; }
# 1. trousseau fscrypt (noyau 4.9) + reglages par defaut
put x4keyring /system/bin/x4keyring 0100755
put x4defaults.sh /system/bin/x4defaults.sh 0100755
put x4keyring.rc /system/etc/init/x4keyring.rc 0100644
CIL=/system/etc/selinux/plat_sepolicy.cil; debugfs -R "dump $CIL plat.cil" "$I"
grep -q 'typepermissive init' plat.cil || printf '\n(typepermissive init)\n' >> plat.cil; put plat.cil $CIL 0100644
# 2. /persist -> /mnt/vendor/persist
debugfs -w -R 'rmdir /persist' "$I" || true; debugfs -w -R 'symlink /persist /mnt/vendor/persist' "$I"
debugfs -w -R 'sif /persist mode 0120644' "$I"; debugfs -w -R 'ea_set -f c3.bin /persist security.selinux' "$I"
# 3. APEX compresses -> decompresses
mkdir -p capx; for c in $(debugfs -R 'ls /system/apex' "$I" 2>/dev/null | tr -s ' ' '\n' | grep '\.capex$'); do n=${c%.capex}
  debugfs -R "dump /system/apex/$c capx/$c" "$I"
  python3 -I -c "import zipfile,sys; open(sys.argv[2],'wb').write(zipfile.ZipFile(sys.argv[1]).read('original_apex'))" capx/$c capx/$n.apex
  debugfs -w -R "rm /system/apex/$c" "$I"; put capx/$n.apex /system/apex/$n.apex 0100644; rm -f capx/$c capx/$n.apex; done
# 4. boutons programmables (WAKE) + appli X4 Boutons (service active au 1er demarrage par x4defaults.sh)
put gpio-keys.kl /system/usr/keylayout/gpio-keys.kl 0100644
debugfs -w -R 'mkdir /system/app/X4Boutons' "$I" 2>/dev/null || true
debugfs -w -R 'sif /system/app/X4Boutons mode 040755' "$I"; debugfs -w -R 'ea_set -f c1.bin /system/app/X4Boutons security.selinux' "$I"
put X4Boutons.apk /system/app/X4Boutons/X4Boutons.apk 0100644
e2fsck -fn "$I"
