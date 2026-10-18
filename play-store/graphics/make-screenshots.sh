#!/usr/bin/env bash
# Builds the captioned Play Store screenshots from the raw captures in raw/.
# Run from anywhere: ./play-store/graphics/make-screenshots.sh
# Needs rsvg-convert and ImageMagick (magick), the same tools as the feature graphic.
set -euo pipefail

cd "$(dirname "$0")"
template=$(<screenshot-template.svg)

# raw capture | output file | caption line 1 | caption line 2
shots=(
  "1-vehicles.png|screenshot-1-vehicles.png|Every car, truck, and|motorcycle in one place"
  "2-due-status.png|screenshot-2-due-status.png|See what is due or|overdue at a glance"
  "3-history.png|screenshot-3-history.png|Service history and|costs by year"
  "4-log-service.png|screenshot-4-log-service.png|Log every service|and repair"
  "5-theme-dark.png|screenshot-5-theme-dark.png|Light or dark,|your choice"
)

for shot in "${shots[@]}"; do
  IFS='|' read -r raw out line1 line2 <<<"$shot"
  image="data:image/png;base64,$(base64 -w0 "raw/$raw")"
  svg=${template//__LINE1__/$line1}
  svg=${svg//__LINE2__/$line2}
  svg=${svg//__IMAGE__/$image}
  # Play rejects screenshots with transparency; strip dates so reruns are byte-identical.
  printf '%s' "$svg" | rsvg-convert \
    | magick - -background '#007AFF' -alpha remove -alpha off -strip \
        -define png:exclude-chunks=date,time PNG24:"$out"
  echo "wrote $out"
done
