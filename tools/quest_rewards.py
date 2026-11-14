"""Apply authored supplies and milestone prizes without resetting claimed rewards."""
import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def apply_rewards(book, uid, registry):
    rules = json.loads((ROOT/'quests/rewards.json').read_text())
    groups = {g['id']: g['title'] for g in json.loads((ROOT/'pack/config/ftbquests/quests/chapter_groups.snbt').read_text())['chapter_groups']}
    report = Counter()
    matched = set()
    for chapter in book.values():
        group = groups.get(chapter.get('group'), '')
        for index, quest in enumerate(chapter['quests']):
            tasks = quest.get('tasks', [])
            if not tasks or any(t['type']=='checkmark' for t in tasks):
                continue
            if quest.get('repeatable'):
                continue
            rewards = quest.setdefault('rewards', [])
            targets = [t.get('entity' if t['type']=='kill' else t['type']) for t in tasks]
            special = next((t for t in targets if isinstance(t,str) and t in rules['milestones']), None)
            material = [r for r in rewards if r['type']=='item']
            milestone = quest.get('shape')=='hexagon'
            # Small intermediate steps may remain unrewarded. XP-only steps get
            # useful supplies, while route endpoints get larger allocations.
            if not special and not rewards and not milestone and index % 3:
                continue
            target = next((t for t in targets if isinstance(t,str)), '')
            ns = target.split(':')[0]
            title = chapter['title'].lower()
            theme = ns if ns in rules['supplies'] else (
                'building' if 'building' in title else
                'farming' if any(w in title for w in ('farm','food','bee')) else
                'settlement' if any(w in title for w in ('settlement','trading')) else
                'technology' if group=='Technology' else
                'magic' if group=='Combat and Magic' else 'exploration')
            if special:
                bundle = rules['milestones'][special]
                matched.add(special)
                report['custom_milestones'] += 1
            elif milestone and group=='Dimensions':
                bundle = [('minecraft:golden_apple',4),('minecraft:diamond',4)]
            else:
                pool = rules['supplies'][theme]
                item,count = pool[int(uid('reward-pool/'+quest['id']),16)%len(pool)]
                bundle = [(item, min(64,count*2) if milestone else count)]
            for slot,(item,count) in enumerate(bundle):
                assert item in registry, 'Unknown quest reward: '+item
                assert 1 <= count <= 64
                # Reuse paid-out slots: upgrading a prize must not make an old
                # claim claimable again. Only genuinely new slots get new IDs.
                if slot < len(material):
                    reward = material[slot]
                    reward.update(item=item,count=count)
                else:
                    reward = dict(id=uid('reward-v2/'+quest['id']+'/'+str(slot)),type='item',item=item,count=count)
                    rewards.append(reward)
                report['item_slots'] += 1
            report['rewarded_quests'] += 1
    allquests = [q for c in book.values() for q in c['quests']]
    report.update({'total_quests':len(allquests),'quests_with_items':sum(any(r['type']=='item' for r in q.get('rewards',[])) for q in allquests), 'xp_only_quests':sum(bool(q.get('rewards')) and all(r['type'] in ('xp','xp_levels') for r in q['rewards']) for q in allquests)})
    result = dict(report)
    result['matched_milestones'] = sorted(matched)
    result['unmatched_milestones'] = sorted(set(rules['milestones'])-matched)
    (ROOT/'docs/quest-rewards.json').write_text(json.dumps(result,indent=2)+'\n')
    return result
