#!/usr/bin/env bash
# Kajakapp CLI — https://kajakapp.com

BASE_URL="https://kajakapp.com"
TOKEN="${KAJAK_TOKEN:-Aa0b48e6f9ef4646b77cf6d702f52d}"
TODAY=$(date -u +%Y-%m-%d)

R='\033[0m'; BOLD='\033[1m'; DIM='\033[2m'
RED='\033[31m'; GRN='\033[32m'; YLW='\033[33m'
BLU='\033[34m'; MAG='\033[35m'; CYN='\033[36m'; WHT='\033[97m'
BG_BLU='\033[44m'; BG_DRK='\033[100m'

die()     { echo -e "${RED}✗ $*${R}" >&2; exit 1; }
hr()      { printf "${DIM}%s${R}\n" "$(printf '─%.0s' $(seq 1 "${1:-64}"))"; }
require() { command -v "$1" &>/dev/null || die "Requires '$1'"; }
header()  { echo; echo -e "${BG_BLU}${BOLD}${WHT}  $*  ${R}"; echo; }
prompt()  { echo -en "${BG_DRK}${WHT} $* ${R} "; }

fetch() {
  local path="$1"; shift
  local url="${BASE_URL}${path}?a=${TOKEN}${*:+&}$*"
  curl -sf --max-time 15 "$url" 2>/dev/null || {
    echo -e "${RED}✗ fetch failed: ${DIM}${path}${R}" >&2; return 1
  }
}

cat_color() {
  case "${1,,}" in
    *world*)    echo "$YLW" ;;
    *european*) echo "$BLU" ;;
    *olympic*)  echo "$MAG" ;;
    *national*) echo "$GRN" ;;
    *)          echo "$CYN" ;;
  esac
}

# ── print one competition line ───────────────────────────────────────────────
print_comp() {
  local n="$1" id="$2" name="$3" loc="$4" start="$5" end="$6" cat="$7" icon="$8"
  local cc; cc=$(cat_color "$cat")
  printf "  ${BOLD}${WHT}%3s${R}  %s ${BOLD}%s${R}\n" "[$n]" "${icon}" "${name}"
  printf "       ${DIM}📍 %-28s 📅 %s → %s${R}\n" "${loc}" "${start}" "${end}"
  printf "       ${cc}%s${R}\n" "${cat}"
  echo
}

# ── competitions menu ────────────────────────────────────────────────────────
menu_competitions() {
  local COMP_JSON
  echo -en "  ${DIM}Loading competitions...${R}\r"
  COMP_JSON=$(fetch "/api/getCompetitions") || return 1

  local mode="${1:-upcoming}"   # upcoming | all | search:<term>
  local page_size=10

  while true; do
    clear
    header "🏆  KAJAKAPP — COMPETITIONS"

    # build filtered list
    local filter_jq
    case "$mode" in
      upcoming)
        filter_jq="map(select(.EndDate >= \"${TODAY}\")) | sort_by(.StartDate)"
        echo -e "  ${GRN}Upcoming competitions${R}  ${DIM}(today: ${TODAY})${R}"
        ;;
      all)
        filter_jq="sort_by(.StartDate) | reverse"
        echo -e "  ${DIM}All competitions, newest first${R}"
        ;;
      search:*)
        local term="${mode#search:}"
        filter_jq="sort_by(.StartDate) | reverse | map(select(
          (.NameEn   | ascii_downcase | contains(\"${term,,}\")) or
          (.LocationEn | ascii_downcase | contains(\"${term,,}\")) or
          (.CompetitionCategory | ascii_downcase | contains(\"${term,,}\"))
        ))"
        echo -e "  ${DIM}Search: \"${term}\"${R}"
        ;;
    esac
    echo

    # collect ALL matching into arrays
    local -a ids names locs starts ends cats icons
    while IFS='|' read -r id name loc start end cat icon; do
      ids+=("$id"); names+=("$name"); locs+=("$loc")
      starts+=("$start"); ends+=("$end"); cats+=("$cat"); icons+=("$icon")
    done < <(echo "$COMP_JSON" | jq -r \
      "${filter_jq}[] | [
        .Id,
        (if .NameEn != \"\" then .NameEn else .NameHu end),
        (if .LocationEn != \"\" then .LocationEn else .LocationHu end),
        .StartDate[:10], .EndDate[:10],
        .CompetitionCategory, .CompetitionIcon
      ] | join(\"|\")")

    local total=${#ids[@]}
    local show=$page_size
    [[ $show -gt $total ]] && show=$total

    if [[ $total -eq 0 ]]; then
      echo -e "  ${YLW}No competitions found.${R}"
      echo
    else
      for ((i=0; i<show; i++)); do
        print_comp $((i+1)) "${ids[$i]}" "${names[$i]}" "${locs[$i]}" \
          "${starts[$i]}" "${ends[$i]}" "${cats[$i]}" "${icons[$i]}"
      done
    fi

    hr 64
    local hint="  ${DIM}[1-${show}]${R} view"
    [[ $total -gt $show ]] && hint+="  ${DIM}[m]${R} more (${show}/${total})"
    hint+="  ${DIM}[u]${R} upcoming  ${DIM}[a]${R} all  ${DIM}[/]${R} search  ${DIM}[q]${R} back"
    echo -e "$hint"
    echo
    prompt "Choice:"
    local choice; read -r choice

    case "$choice" in
      q|Q|"") return ;;
      u|U) mode="upcoming"; page_size=10 ;;
      a|A) mode="all";      page_size=10 ;;
      m|M) page_size=$(( page_size + 10 )) ;;
      /*)  mode="search:${choice:1}"; page_size=10 ;;
      ''[0-9]*|[0-9]*)
        local idx=$(( choice - 1 ))
        if [[ $idx -ge 0 && $idx -lt $show ]]; then
          menu_competition_detail "${ids[$idx]}" "${names[$idx]}" "${icons[$idx]}"
        else
          echo -e "  ${RED}Invalid number${R}"; sleep 0.8
        fi
        ;;
      *) mode="search:${choice}"; page_size=10 ;;
    esac
  done
}

# ── competition detail: one getCompetition call, paginate locally ─────────────
menu_competition_detail() {
  local comp_id="$1" comp_name="$2" comp_icon="$3"

  clear; header "${comp_icon}  ${comp_name}"
  echo -en "  ${DIM}Loading races...${R}"

  local comp_json
  comp_json=$(fetch "/api/getCompetition" "competitionId=${comp_id}") || { sleep 1; return; }

  if [[ "$comp_json" == "{}" || -z "$comp_json" ]]; then
    echo -e "\n  ${YLW}No data available.${R}"
    prompt "Press Enter to go back"; read -r; return
  fi

  # Parse all races from Races dict (keys are numeric strings)
  local -a race_ids race_names race_rounds race_dts race_cats race_best race_fin
  while IFS='|' read -r rid name round cat best fin sd; do
    race_ids+=("$rid")
    race_names+=("$name")
    race_rounds+=("$round")
    race_cats+=("$cat")
    race_best+=("$best")
    race_fin+=("$fin")
    race_dts+=("$(date -d "${sd}" '+%m-%d %H:%M' 2>/dev/null || echo "${sd:5:11}")")
  done < <(echo "$comp_json" | jq -r '
    .Races | to_entries | sort_by(.key | tonumber) | .[] |
    [
      .key,
      .value.Race.Name,
      .value.Race.Round,
      (.value.Race.RaceCategory // ""),
      (.value.Race.IsBestFinal | tostring),
      (.value.IsFinished | tostring),
      .value.Race.StartDate
    ] | join("|")
  ')

  local total=${#race_ids[@]}
  local page_size=10 offset=0

  while true; do
    clear
    header "${comp_icon}  ${comp_name}"

    if [[ $total -eq 0 ]]; then
      echo -e "  ${YLW}No races found.${R}"
      prompt "Press Enter to go back"; read -r; return
    fi

    local end=$(( offset + page_size ))
    [[ $end -gt $total ]] && end=$total
    local more_hint="" prev_hint=""
    [[ $end -lt $total ]]  && more_hint="  ${DIM}[m]${R} more"
    [[ $offset -gt 0 ]]    && prev_hint="  ${DIM}[p]${R} prev"

    echo -e "  ${DIM}Races $((offset+1))–${end} of ${total}${R}"
    echo
    for ((i=offset; i<end; i++)); do
      local fin_mark="";  [[ "${race_fin[$i]}"  == "true" ]] && fin_mark=" ${GRN}✓${R}"
      local best_mark=""; [[ "${race_best[$i]}" == "true" ]] && best_mark=" ${YLW}★${R}"
      local cc; cc=$(cat_color "${race_cats[$i]}")
      printf "  ${BOLD}${WHT}%3s${R}  ${BOLD}%s${R}  ${DIM}%s${R}%b%b  ${cc}%s${R}\n" \
        "$((i+1))" "${race_names[$i]}" "${race_rounds[$i]}" "$fin_mark" "$best_mark" "${race_dts[$i]}"
    done

    echo
    hr 64
    echo -e "  ${DIM}[1-${total}]${R} view race${more_hint}${prev_hint}  ${DIM}[q]${R} back"
    echo
    prompt "Choice:"; read -r choice

    case "$choice" in
      q|Q|"") return ;;
      m|M) [[ $end -lt $total ]] && offset=$(( offset + page_size )) ;;
      p|P) [[ $offset -gt 0 ]]  && offset=$(( offset - page_size < 0 ? 0 : offset - page_size )) ;;
      ''[0-9]*|[0-9]*)
        local idx=$(( choice - 1 ))
        if [[ $idx -ge 0 && $idx -lt $total ]]; then
          echo -en "\n  ${DIM}Loading race detail...${R}"
          local rjson
          rjson=$(fetch "/api/getRace" "competitionId=${comp_id}&raceId=${race_ids[$idx]}")
          _show_race_json "$rjson" "$comp_id" "${race_ids[$idx]}"
        else
          echo -e "  ${RED}Invalid number${R}"; sleep 0.8
        fi
        ;;
    esac
  done
}

# ── athletes menu ─────────────────────────────────────────────────────────────
menu_athletes() {
  local ATH_JSON
  echo -en "  ${DIM}Loading athletes...${R}\r"
  ATH_JSON=$(fetch "/api/getAthletes") || return 1

  local search_term=""

  while true; do
    clear
    header "🏅  KAJAKAPP — ATHLETES"

    local -a aids anames anations aborns aclubs aranks aemojis
    aids=(); anames=(); anations=(); aborns=(); aclubs=(); aranks=(); aemojis=()

    if [[ -z "$search_term" ]]; then
      echo -e "  ${DIM}Enter a name or nation to search (e.g. \"Gazdag\", \"HUN\", \"GER\")${R}"
      echo
      hr 64
      echo -e "  ${DIM}[/]${R} search  ${DIM}[q]${R} back"
      echo
      prompt "Search athletes:"; read -r search_term
      [[ "$search_term" == "q" || "$search_term" == "Q" || -z "$search_term" ]] && return
      [[ "$search_term" == /* ]] && search_term="${search_term:1}"
      continue
    fi

    # filter
    while IFS='|' read -r id name nation born club rank emoji; do
      aids+=("$id"); anames+=("$name"); anations+=("$nation"); aborns+=("$born")
      aclubs+=("$club"); aranks+=("$rank"); aemojis+=("$emoji")
    done < <(echo "$ATH_JSON" | jq -r --arg q "${search_term,,}" '
      .[] | select(
        (.Name   | ascii_downcase | contains($q)) or
        (.Nation | ascii_downcase | contains($q)) or
        (.Club   | ascii_downcase | contains($q))
      ) |
      [.Id, .Name, .Nation, .BirthYear, .Club, (.IcfWorldRank|tostring), (.KajakappEmoji // "")]
      | join("|")
    ')

    local count=${#aids[@]}
    echo -e "  ${DIM}Search: \"${search_term}\"  —  ${count} result(s)${R}"
    echo

    if [[ $count -eq 0 ]]; then
      echo -e "  ${YLW}No athletes found.${R}"
    elif [[ $count -gt 40 ]]; then
      echo -e "  ${YLW}Too many results (${count}). Narrow your search.${R}"
    else
      local i
      for ((i=0; i<count; i++)); do
        local rank_str=""
        [[ "${aranks[$i]}" != "-1" ]] && rank_str="  ${YLW}🌍 #${aranks[$i]}${R}"
        local em="${aemojis[$i]}"
        printf "  ${BOLD}${WHT}%3s${R}  %s${BOLD}%s${R}  ${CYN}%s${R}  ${DIM}%s${R}%b\n" \
          "$((i+1))" "${em:+${em} }" "${anames[$i]}" "${anations[$i]}" "${aborns[$i]}" "$rank_str"
        [[ -n "${aclubs[$i]}" ]] && printf "       ${DIM}%s${R}\n" "${aclubs[$i]}"
        echo
      done
    fi

    hr 64
    echo -e "  ${DIM}[1-${count}]${R} view profile  ${DIM}[/]${R} new search  ${DIM}[q]${R} back"
    echo
    prompt "Choice:"; read -r choice

    case "$choice" in
      q|Q|"") return ;;
      /*) search_term="${choice:1}" ;;
      ''[0-9]*|[0-9]*)
        local idx=$(( choice - 1 ))
        if [[ $idx -ge 0 && $idx -lt $count ]]; then
          show_athlete "${aids[$idx]}"
        else
          echo -e "  ${RED}Invalid number${R}"; sleep 0.8
        fi
        ;;
      *) search_term="$choice" ;;
    esac
  done
}

# ── athlete profile ───────────────────────────────────────────────────────────
show_athlete() {
  local athlete_id="$1"
  echo -en "  ${DIM}Loading athlete...${R}\r"
  local json
  json=$(fetch "/api/getAthlete" "athleteId=${athlete_id}") || { sleep 1; return; }

  local name nation born club rank points emoji aliases
  name=$(echo    "$json" | jq -r '.Athlete.Name')
  nation=$(echo  "$json" | jq -r '.Athlete.Nation')
  born=$(echo    "$json" | jq -r '.Athlete.BirthYear')
  club=$(echo    "$json" | jq -r '.Athlete.Club // ""')
  rank=$(echo    "$json" | jq -r '.Athlete.IcfWorldRank')
  points=$(echo  "$json" | jq -r '.Athlete.IcfWorldPoints')
  emoji=$(echo   "$json" | jq -r '.Athlete.KajakappEmoji // ""')
  aliases=$(echo "$json" | jq -r '.Athlete.Aliases[]?' 2>/dev/null | paste -sd ', ')

  # Parse races (sorted newest first)
  local -a r_comp_id r_race_id r_icon r_comp r_name r_round r_dt r_pos r_time r_fin
  while IFS='|' read -r cid rid icon comp rname round sd pos time fin; do
    r_comp_id+=("$cid"); r_race_id+=("$rid"); r_icon+=("$icon")
    r_comp+=("$comp");   r_name+=("$rname"); r_round+=("$round")
    r_pos+=("$pos");     r_time+=("$time");  r_fin+=("$fin")
    r_dt+=("$(date -d "${sd}" '+%Y-%m-%d' 2>/dev/null || echo "${sd:0:10}")")
  done < <(echo "$json" | jq -r '
    .Races | to_entries
    | sort_by(.value.Race.StartDate) | reverse | .[]
    | [
        .value.Race.Competition.Id,
        .value.Race.Id,
        (.value.Race.Competition.CompetitionIcon // ""),
        (if (.value.Race.Competition.NameEn // "") != ""
         then .value.Race.Competition.NameEn
         else .value.Race.Competition.NameHu end),
        .value.Race.Name,
        (.value.Race.Round // ""),
        .value.Race.StartDate,
        (.value.FinishPosition // ""),
        (.value.FinishTime // ""),
        (.value.IsFinished | tostring)
      ] | join("|")
  ' 2>/dev/null)

  local total=${#r_comp_id[@]} page_size=10 offset=0

  while true; do
    clear
    header "🏅  ${emoji:+${emoji} }${name}"
    echo -e "  ${CYN}${nation}${R}  ${DIM}born ${born}${R}"
    [[ -n "$club" ]]      && echo -e "  ${DIM}🏫 ${club}${R}"
    [[ "$rank" != "-1" ]] && echo -e "  ${YLW}🌍 ICF world rank #${rank}  (${points} pts)${R}"
    [[ -n "$aliases" ]]   && echo -e "  ${DIM}Also known as: ${aliases}${R}"
    echo -e "  ${DIM}ID: ${athlete_id}${R}"
    echo; hr 64; echo

    if [[ $total -eq 0 ]]; then
      echo -e "  ${DIM}No race history available.${R}"
      echo
      prompt "Press Enter to go back"; read -r; return
    fi

    echo -e "  ${BOLD}Race history (${total})${R}"
    echo

    local end=$(( offset + page_size ))
    [[ $end -gt $total ]] && end=$total

    for (( i=offset; i<end; i++ )); do
      local num=$(( i + 1 ))
      local pos_str=""
      local pos="${r_pos[$i]}"
      if [[ "${r_fin[$i]}" == "true" && -n "$pos" ]]; then
        case "$pos" in
          "1.") pos_str=" ${YLW}🥇 ${pos}${R}" ;;
          "2.") pos_str=" ${WHT}🥈 ${pos}${R}" ;;
          "3.") pos_str=" ${YLW}🥉 ${pos}${R}" ;;
          *)    pos_str=" ${DIM}${pos}${R}" ;;
        esac
      elif [[ "${r_fin[$i]}" != "true" ]]; then
        pos_str=" ${DIM}upcoming${R}"
      fi
      local time_str=""
      [[ -n "${r_time[$i]}" ]] && time_str="  ${DIM}${r_time[$i]}${R}"
      printf "  ${DIM}%2d.${R}  ${CYN}%s${R}  ${DIM}%s${R}\n" \
        "$num" "${r_dt[$i]}" "${r_icon[$i]} ${r_comp[$i]}"
      printf "       ${BOLD}%s${R}  ${DIM}%s${R}%b%b\n" \
        "${r_name[$i]}" "${r_round[$i]}" "$pos_str" "$time_str"
      echo
    done

    local hints=""
    [[ $offset -gt 0 ]]    && hints+="[p] prev  "
    [[ $end -lt $total ]]  && hints+="[m] more  "
    hints+="[q] back"
    prompt "${hints}  > "; read -r choice
    case "$choice" in
      m|M) [[ $end -lt $total ]] && offset=$(( offset + page_size )) ;;
      p|P) [[ $offset -gt 0 ]]   && offset=$(( offset - page_size )) ;;
      q|Q|"") return ;;
      *)
        if [[ "$choice" =~ ^[0-9]+$ ]]; then
          local idx=$(( choice - 1 ))
          if (( idx >= 0 && idx < total )); then
            echo -en "  ${DIM}Loading race...${R}\r"
            local race_json
            race_json=$(fetch "/api/getRace" \
              "competitionId=${r_comp_id[$idx]}&raceId=${r_race_id[$idx]}") || { sleep 1; continue; }
            _show_race_json "$race_json" "${r_comp_id[$idx]}" "${r_race_id[$idx]}"
          fi
        fi
        ;;
    esac
  done
}

# ── upcoming races (across competitions) ─────────────────────────────────────
menu_upcoming_races() {
  local n="${1:-10}"
  local COMP_JSON
  echo -en "  ${DIM}Loading...${R}\r"
  COMP_JSON=$(fetch "/api/getCompetitions") || return 1

  while true; do
    clear
    header "📅  UPCOMING RACES  (next ${n})"

    # Get competitions with upcoming or ongoing events, sorted by start date
    local -a comp_ids comp_names comp_icons
    while IFS='|' read -r id name icon; do
      comp_ids+=("$id"); comp_names+=("$name"); comp_icons+=("$icon")
    done < <(echo "$COMP_JSON" | jq -r --arg today "$TODAY" '
      map(select(.EndDate >= $today)) | sort_by(.StartDate)[] |
      [
        .Id,
        (if .NameEn != "" then .NameEn else .NameHu end),
        .CompetitionIcon
      ] | join("|")
    ')

    local found=0
    local -a rdisplay_comp rdisplay_race rdisplay_compid
    rdisplay_comp=(); rdisplay_race=(); rdisplay_compid=()

    for i in "${!comp_ids[@]}"; do
      [[ $found -ge $n ]] && break
      local cid="${comp_ids[$i]}"
      local cname="${comp_names[$i]}"
      local cicon="${comp_icons[$i]}"

      # Fetch races for this competition starting from race 1
      local rid=1
      while [[ $found -lt $n ]]; do
        local rjson
        rjson=$(fetch "/api/getRace" "competitionId=${cid}&raceId=${rid}" 2>/dev/null)
        [[ "$rjson" == "{}" || -z "$rjson" ]] && break

        local rstart rname rround rfinished
        rstart=$(echo    "$rjson" | jq -r '.Race.StartDate')
        rname=$(echo     "$rjson" | jq -r '.Race.Name')
        rround=$(echo    "$rjson" | jq -r '.Race.Round')
        rfinished=$(echo "$rjson" | jq -r '.IsFinished')
        local rbest; rbest=$(echo "$rjson" | jq -r '.Race.IsBestFinal')

        # Only show non-finished races
        if [[ "$rfinished" != "true" ]]; then
          local dt; dt=$(date -d "${rstart}" '+%m-%d %H:%M' 2>/dev/null || echo "${rstart:5:11}")
          found=$(( found + 1 ))
          rdisplay_compid+=("$cid")

          local best_mark=""; [[ "$rbest" == "true" ]] && best_mark=" ${YLW}★${R}"
          local line="${cicon} ${BOLD}${cname}${R}${best_mark}"$'\n'"     ${WHT}${rname}${R}  ${DIM}${rround}${R}  ${DIM}📅 ${dt}${R}"
          rdisplay_race+=("$line")
          rdisplay_comp+=("${cid}|${rid}")

          printf "  ${BOLD}${WHT}%3s${R}  %s\n\n" "$found" "$(echo -e "$line")"
        fi
        rid=$(( rid + 1 ))
      done
    done

    [[ $found -eq 0 ]] && echo -e "  ${YLW}No upcoming races found.${R}\n"

    hr 64
    echo -e "  ${DIM}[1-${found}]${R} view race  ${DIM}[+5]${R} load more  ${DIM}[q]${R} back"
    echo
    prompt "Choice:"; read -r choice

    case "$choice" in
      q|Q|"") return ;;
      +*) n=$(( n + ${choice:1} )) ;;
      ''[0-9]*|[0-9]*)
        local idx=$(( choice - 1 ))
        if [[ $idx -ge 0 && $idx -lt $found ]]; then
          local parts; IFS='|' read -r cid rid <<< "${rdisplay_comp[$idx]}"
          local cname="${comp_names[$idx]}" cicon="${comp_icons[$idx]}"
          # find the comp name from comp_ids
          for j in "${!comp_ids[@]}"; do
            [[ "${comp_ids[$j]}" == "$cid" ]] && { cname="${comp_names[$j]}"; cicon="${comp_icons[$j]}"; break; }
          done
          # show that race directly
          local rjson; rjson=$(fetch "/api/getRace" "competitionId=${cid}&raceId=${rid}")
          _show_race_json "$rjson" "$cid" "$rid"
        fi
        ;;
    esac
  done
}

# ── render a fetched race JSON (shared by multiple paths) ─────────────────────
_show_race_json() {
  local json="$1" comp_id="$2" race_id="$3"
  clear
  local name round startdate best finished cat
  name=$(echo      "$json" | jq -r '.Race.Name')
  round=$(echo     "$json" | jq -r '.Race.Round')
  startdate=$(echo "$json" | jq -r '.Race.StartDate')
  best=$(echo      "$json" | jq -r '.Race.IsBestFinal')
  finished=$(echo  "$json" | jq -r '.IsFinished')
  cat=$(echo       "$json" | jq -r '.Race.RaceCategory // ""')
  local dt; dt=$(date -d "${startdate}" '+%Y-%m-%d %H:%M' 2>/dev/null || echo "${startdate:0:16}")
  local status_str=""
  [[ "$finished" == "true" ]]  && status_str="  ${GRN}✓ Finished${R}"
  [[ "$finished" == "false" ]] && status_str="  ${YLW}⏳ Upcoming${R}"

  header "🛶  ${name}  —  ${round}"
  echo -e "  ${DIM}📅 ${dt}${R}${status_str}"
  [[ -n "$cat" ]]         && echo -e "  $(cat_color "$cat")${cat}${R}"
  [[ "$best" == "true" ]] && echo -e "  ${YLW}★ Best Final${R}"
  echo; hr 64

  local boat_count; boat_count=$(echo "$json" | jq '.Boats | length')
  if [[ "$boat_count" -eq 0 ]]; then
    echo -e "  ${DIM}No startlist/results yet.${R}"
  else
    printf "  ${BOLD}%-4s  %-12s  %-10s  %-6s  %-36s  %s${R}\n" "POS" "TIME" "+DELTA" "LANE" "ATHLETE(S)" "NAT"
    hr 64
    echo "$json" | jq -r '
      .Boats[] |
      [
        .FinishPosition, .FinishTime, .FinishTimeDelta, .StartNumber,
        (.Athletes | map(.Name + (if .KajakappEmoji != "" then " \(.KajakappEmoji)" else "" end)) | join(" / ")),
        (.Athletes | map(.Nation) | unique | join("/"))
      ] | join("|")
    ' | while IFS='|' read -r pos time delta lane athletes nation; do
      local medal=""; case "$pos" in 1) medal="🥇";; 2) medal="🥈";; 3) medal="🥉";; DNS|DNF|DSQ) medal="❌";; "") medal="⏳"; pos="—";; esac
      local pc="$R"; case "$pos" in 1) pc="${BOLD}${YLW}";; 2) pc="${BOLD}${WHT}";; 3) pc="${BOLD}${MAG}";; DNS|DNF|DSQ) pc="${RED}";; esac
      local delta_str; [[ -n "$delta" ]] && delta_str="+${delta}s"
      printf "  %s ${pc}%-4s${R}  ${GRN}%-12s${R}  ${DIM}%-10s${R}  ${DIM}%-6s${R}  ${WHT}%-36s${R}  ${CYN}%s${R}\n" \
        "$medal" "$pos" "$time" "$delta_str" "$lane" "$athletes" "$nation"
    done
  fi
  echo; hr 64
  echo -e "  ${DIM}Competition: ${comp_id}  Race: ${race_id}${R}"
  echo
  prompt "Press Enter to go back"; read -r
}

# ── main menu ─────────────────────────────────────────────────────────────────
menu_main() {
  while true; do
    clear
    echo
    echo -e "${BG_BLU}${BOLD}${WHT}  🛶  KAJAKAPP  ${R}"
    echo -e "  ${DIM}https://kajakapp.com${R}"
    echo
    hr 40
    echo
    echo -e "  ${BOLD}${WHT}[1]${R}  🏆  Competitions"
    echo -e "  ${BOLD}${WHT}[2]${R}  📅  Upcoming races"
    echo -e "  ${BOLD}${WHT}[3]${R}  🏅  Athletes"
    echo -e "  ${BOLD}${WHT}[q]${R}  Exit"
    echo
    hr 40
    echo
    prompt "Choose:"; read -r choice

    case "$choice" in
      1) menu_competitions "upcoming" ;;
      2) menu_upcoming_races 10 ;;
      3) menu_athletes ;;
      q|Q|0) echo; exit 0 ;;
    esac
  done
}

# ── main ─────────────────────────────────────────────────────────────────────
[[ "${BASH_SOURCE[0]}" != "$0" ]] && return 0

require curl
require jq

case "${1:-}" in
  "")               menu_main ;;
  competitions|c)   shift; menu_competitions "${1:-upcoming}" ;;
  athletes)         shift; menu_athletes ;;
  athlete)          shift; show_athlete "$1" ;;
  race)             shift
    echo -en "  ${DIM}Loading...${R}\r"
    json=$(fetch "/api/getRace" "competitionId=$1${2:+&raceId=$2}")
    echo "$json" | jq . ;;
  help|-h|--help)
    echo -e "${BOLD}kajak.sh${R} — Kajakapp CLI"
    echo -e "  ${GRN}(no args)${R}              interactive menu"
    echo -e "  ${GRN}competitions${R}           upcoming competitions menu"
    echo -e "  ${GRN}athletes${R}               athlete search menu"
    echo -e "  ${GRN}athlete <id>${R}           show athlete profile"
    echo -e "  ${GRN}race <compId> [raceId]${R} raw race JSON"
    echo
    echo -e "  ${DIM}KAJAK_TOKEN env var overrides token${R}"
    ;;
  *) echo -e "${RED}Unknown: $1. Try: $0 help${R}" >&2; exit 1 ;;
esac
