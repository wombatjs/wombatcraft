#!/bin/bash
set -euo pipefail
action=${1:-apply}
ipt=/usr/sbin/iptables
remove_rule() { "$ipt" "$@" 2>/dev/null || true; }
remove() {
 remove_rule -D DOCKER-USER -j WOMBATCRAFT
 remove_rule -t nat -D PREROUTING -j WOMBATCRAFT_IN
 remove_rule -t nat -D POSTROUTING -j WOMBATCRAFT_OUT
 remove_rule -F WOMBATCRAFT
 remove_rule -X WOMBATCRAFT
 remove_rule -t nat -F WOMBATCRAFT_IN
 remove_rule -t nat -X WOMBATCRAFT_IN
 remove_rule -t nat -F WOMBATCRAFT_OUT
 remove_rule -t nat -X WOMBATCRAFT_OUT
}
if [[ $action == remove ]]; then remove; exit 0; fi
[[ $action == apply ]] || exit 2
remove
trap remove ERR
"$ipt" -N WOMBATCRAFT
"$ipt" -t nat -N WOMBATCRAFT_IN
"$ipt" -t nat -N WOMBATCRAFT_OUT
# The socket-activated relay handles Minecraft TCP. Keep the UDP voice route.
for spec in udp:24454; do
 proto=${spec%:*}; port=${spec#*:}
 "$ipt" -A WOMBATCRAFT -i ens6 -o wg-fivem -d 10.77.5.2/32 -p "$proto" --dport "$port" -j ACCEPT
 "$ipt" -A WOMBATCRAFT -i wg-fivem -o ens6 -s 10.77.5.2/32 -p "$proto" --sport "$port" -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT
 "$ipt" -t nat -A WOMBATCRAFT_IN -i ens6 -d 74.208.117.88/32 -p "$proto" --dport "$port" -j DNAT --to-destination "10.77.5.2:$port"
 "$ipt" -t nat -A WOMBATCRAFT_OUT -o wg-fivem -d 10.77.5.2/32 -p "$proto" --dport "$port" -j SNAT --to-source 10.77.5.1
done
"$ipt" -A WOMBATCRAFT -j RETURN
"$ipt" -I DOCKER-USER 1 -j WOMBATCRAFT
"$ipt" -t nat -I PREROUTING 1 -j WOMBATCRAFT_IN
"$ipt" -t nat -I POSTROUTING 1 -j WOMBATCRAFT_OUT
