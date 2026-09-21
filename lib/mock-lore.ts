import {
  LoreItemData,
  LoreMatchChunk,
  LoreSearchPage,
  LoreSearchResult,
} from "@/types/search";

export const MOCK_LORE_ITEMS: LoreItemData[] = [
  {
    id: 1,
    title: "The Founding of Silverhold",
    description: "History of the city of Silverhold.",
    date: "2024-01-10",
    author: "Aldric",
    byAdmin: true,
    isViewed: true,
    nonPublic: false,
    tags: [{ id: 1, name: "History" }],
    content: `## Origins

Silverhold was founded in the third age by a coalition of merchants fleeing the collapse of the Ember Empire. They chose the valley between the Greymist Mountains and the Silver River for its natural defenses and access to silver ore, which would give the city its name.

The founders drafted the **Charter of the Open Hand**, which guaranteed freedom of trade and outlawed slavery within the city walls. This charter became the bedrock of Silverhold's culture for centuries.

Over time, the merchant families consolidated power into the Council of Five — five guilds that to this day control taxation, law enforcement, and foreign relations. The silver trade made Silverhold wealthy beyond measure, and that wealth attracted enemies who sought to claim the silver for themselves.

The great siege of Silverhold in 412 AC lasted seven months. The defenders used the silver mines themselves as an escape route, collapsing tunnels behind them to deny the attackers access to the silver veins beneath the mountain.`,
  },
  {
    id: 2,
    title: "The Curse of the Moonwell",
    description: "A dark legend about the Moonwell.",
    date: "2024-02-15",
    author: "Miriel",
    byAdmin: false,
    isViewed: false,
    nonPublic: false,
    tags: [{ id: 2, name: "Magic" }],
    content: `## The Moonwell

Deep in the Whisperwood lies the Moonwell — a pool of water that reflects the moon even in daylight. Local legends say it was cursed by the witch Seravane after her daughter drowned there.

Anyone who drinks from the Moonwell at midnight is said to gain visions of their own death. Three adventurers who drank from it in the Year of the Broken Crown reportedly all died exactly as they had seen. The curse is believed to be tied to the moon's cycle — during the new moon the curse is said to be dormant, and the water tastes of nothing but cold spring water.

The curse can supposedly be lifted only by pouring the blood of a willing sacrifice into the water. No one has attempted this in living memory. Scholars of the Amber Order have tried to study the Moonwell but found that their instruments behave strangely near it — compasses spin, fire refuses to light, and the moon appears to pulse.

A second Moonwell is rumored to exist somewhere north of the Greymist Mountains, though no expedition has confirmed its location. Some say the two wells are connected, and that a vision seen in one can be averted by drinking from the other before the next full moon.`,
  },
  {
    id: 3,
    title: "Dwarven Rune-Smith Traditions",
    description: "Practices of dwarven rune crafting.",
    date: "2024-03-01",
    author: "Borin Stoneback",
    byAdmin: true,
    isViewed: null,
    nonPublic: false,
    tags: [
      { id: 3, name: "Crafting" },
      { id: 4, name: "Dwarves" },
    ],
    content: `## Rune-Smithing

The dwarven art of rune-smithing dates back to the time before memory, passed from master to apprentice in unbroken lineages. Each clan guards its runes jealously — some runes are considered sacred secrets, never inscribed for outsiders.

A rune-smith apprenticeship lasts **twelve years**. The first three years are spent only in observation; the apprentice may not touch a tool. Years four through eight are devoted to carving inert runes on stone. Only in the final four years does the master reveal the methods of *awakening* a rune — binding a fragment of elemental will into the mark.

Runes of the Stonemaw clan are notably more aggressive in nature, reflecting their clan's warrior culture. Their signature rune — the Bite — causes metal engraved with it to grow hot when enemies are near. The Stonemaw rune-smiths keep the Bite formula locked in a vault sealed with three separate rune-locks, each held by a different clan elder. A rune-smith who shares the Bite formula with an outsider faces exile and the erasure of their lineage from the clan records.

The Deepdelve clan, by contrast, specializes in runes of perception and navigation. Their Wayfinder rune, carved into the soles of boots, is said to always know the shortest path to open sky. It is their most traded rune and the clan's primary export. Even so, no outsider has ever been taught to inscribe a Wayfinder rune — the Deepdelve sell only finished goods, never the knowledge behind them.`,
  },
  {
    id: 4,
    title: "The Red Plague of 847",
    description: "A deadly disease that swept the region.",
    date: "2024-03-22",
    author: "Seraphina Voss",
    byAdmin: false,
    isViewed: true,
    nonPublic: true,
    tags: [
      { id: 1, name: "History" },
      { id: 5, name: "Medicine" },
    ],
    content: `## The Red Plague

In 847 by the Common Calendar, a plague of unknown origin swept from the port city of Duskfall across the entire eastern seaboard. Victims first exhibited bright red splotches on their skin, followed by fever, then hemorrhagic collapse within seven days.

Healers of the Amber Order identified that the disease spread through contaminated water supplies. The city of Silverhold was largely spared because its aqueduct was sealed by order of the Council of Five. Other cities were not so fortunate — Duskfall lost over half its population in the first three months, and the port fell silent as no ships dared to dock.

The plague killed an estimated **one in four** people in the affected regions over eighteen months. It is considered the single deadliest event in recorded history east of the Greymist Mountains. Some scholars believe it was conjured deliberately as an act of war, though no enemy has ever been confirmed.

A second wave of the Red Plague struck twenty years later, in 867, but proved far less lethal. The Amber Order had by then developed a treatment — a bitter tea brewed from ironbark root that, taken at the first sign of red splotches, could prevent the hemorrhagic phase. The Order distributed this treatment freely, which went a long way toward restoring their reputation after the controversy of the first wave, when critics accused them of withholding cures.`,
  },
  {
    id: 5,
    title: "The Amber Order",
    description: "A healing and scholarly organization.",
    date: "2024-04-05",
    author: "Aldric",
    byAdmin: true,
    isViewed: null,
    nonPublic: false,
    tags: [
      { id: 5, name: "Medicine" },
      { id: 6, name: "Organizations" },
    ],
    content: `## Structure and Purpose

The Amber Order is a continent-wide organization of healers, scholars, and archivists. Founded two centuries ago by the healer Thessaly Vorn, its original mission was to catalog all known diseases and their treatments.

Today the Order operates **forty-three** chapter houses across the known world. Each chapter house maintains an infirmary, a library, and a training hall. Membership is open to any who can pass the Trials of Compassion — a grueling week-long test of medical skill and ethical judgment.

The Order is notably neutral in political matters and will treat wounded soldiers regardless of which side they fight for. This neutrality has occasionally made them targets of suspicion, but it has also allowed them to operate in active war zones where no other organization could survive.

The Order's library in their central chapter house — located in the free city of Miremount — is said to contain records of every plague and epidemic since the founding of civilization. Access to the restricted archives requires the rank of Senior Archivist, a title held by fewer than twenty scholars alive today. Rumor holds that the restricted archives contain not only cures but also descriptions of diseases that have never been seen in the wild — diseases the Order is believed to have created themselves for research purposes.`,
  },
  {
    id: 6,
    title: "The Silver River Compact",
    description: "A trade agreement between river cities.",
    date: "2024-04-18",
    author: "Miriel",
    byAdmin: false,
    isViewed: false,
    nonPublic: false,
    tags: [
      { id: 1, name: "History" },
      { id: 7, name: "Politics" },
    ],
    content: `## The Compact

The Silver River Compact was signed in 601 AC between seven cities along the Silver River, establishing shared rules for river trade, toll collection, and dispute resolution. It is still in effect today and governs most commerce on the river.

The Compact created the River Court — a rotating tribunal of one magistrate from each signatory city that convenes twice yearly to hear commercial disputes. Rulings of the River Court are binding on all signatories, though enforcement varies in practice.

Silver River toll gates are perhaps the most visible legacy of the Compact. Each toll gate is jointly staffed by two guards — one from the city upstream, one from the city downstream — and the revenue is split equally between the two. This arrangement has prevented more than a few wars over disputed stretches of river.

The Compact was nearly dissolved in 744 AC when Silverhold attempted to divert a tributary of the Silver River to power a new mill district. The downstream city of Floodgate blockaded the river for six weeks until Silverhold agreed to pay reparations and restore the tributary. The incident is still studied as a case study in compact enforcement.`,
  },
  {
    id: 7,
    title: "Whisperwood Spirits",
    description: "Entities said to inhabit the Whisperwood.",
    date: "2024-05-02",
    author: "Seraphina Voss",
    byAdmin: false,
    isViewed: null,
    nonPublic: false,
    tags: [
      { id: 2, name: "Magic" },
      { id: 8, name: "Folklore" },
    ],
    content: `## The Spirits

The Whisperwood is named for the constant low sound that travelers report hearing within it — a soft whisper that seems to come from no particular direction. Local people believe this whispering is the voices of spirits that inhabit the wood.

Three types of spirit are commonly described. The first are the Watchers — tall, pale figures seen only at the edge of torchlight, always at a distance, never approaching. They are said to observe travelers but never interfere. The second are the Guides — voices that call out from the dark in the voice of someone the traveler loves, luring them deeper into the wood. The third, and rarest, are the Anchors — ancient trees whose bark has faces carved into it by no human hand, which are said to protect travelers who sleep at their roots.

The Moonwell lies at the center of the Whisperwood, and many scholars believe the spirits are connected to it. Some say the Watchers are the ghosts of those who died after drinking from the Moonwell, doomed to wander the wood forever. Others say the Guides are the witch Seravane's servants, sent to bring new victims to the well.

A hunter named Corrigan who spent three days lost in the Whisperwood claimed to have spoken with a Watcher. According to Corrigan, the spirit told him that the whispering is not voices but memory — the sound of everything that has ever happened in the wood, playing on an endless loop.`,
  },
  {
    id: 8,
    title: "The Stonemaw Clan Wars",
    description: "A series of conflicts among dwarven clans.",
    date: "2024-05-15",
    author: "Borin Stoneback",
    byAdmin: true,
    isViewed: true,
    nonPublic: false,
    tags: [
      { id: 4, name: "Dwarves" },
      { id: 1, name: "History" },
    ],
    content: `## The Clan Wars

The Stonemaw Clan Wars were a series of five conflicts fought over two centuries between the Stonemaw and Deepdelve clans. The root cause was control of the Deepvein — a vast ore deposit that lay beneath territory claimed by both clans.

The First Stonemaw War lasted eleven years and ended in a stalemate. Both sides had used rune-forged weapons extensively, and the resulting devastation convinced neither side they could win outright. The ceasefire gave each clan control of half the Deepvein.

The Second and Third Wars were shorter — sparked by specific incidents (a rune-smith who crossed clan lines, a cave-in that was suspected sabotage) rather than strategic calculations. Both ended quickly and inconclusively.

The Fourth War was the bloodiest. The Stonemaw developed a new weapon — the Rupture rune, which could cause stone to fracture violently at range. They used it to collapse Deepdelve tunnels, killing hundreds. The Deepdelve responded by flooding Stonemaw tunnels using diverted underground rivers. The resulting destruction left the Deepvein itself partially inaccessible for decades.

The Fifth War never happened. On the eve of what would have been its opening battle, both clan elders died within hours of each other under circumstances that have never been explained. Their successors, perhaps shaken by the coincidence, agreed to the Deepvein Partition Treaty, which has held for sixty years.`,
  },
];

// ─── Highlight marker constants ───────────────────────────────────────────────

/**
 * Unicode Private Use Area characters used by the backend to bracket match spans.
 * PUA chars survive markdown parsers intact (unlike \u0001/\u0002 which get stripped).
 */
export const HIGHLIGHT_START = "\u0001";
export const HIGHLIGHT_END = "\u0002";

// ─── Search helpers ───────────────────────────────────────────────────────────

/**
 * X symbols of context captured around each hit position before merging.
 * If two hit windows overlap they become one chunk.
 */
const CONTEXT_RADIUS = 120;

/**
 * Y symbols — the final chunk is trimmed to this max length after merging.
 * Must be > 2 * CONTEXT_RADIUS to guarantee merged overlaps fit.
 */
const CHUNK_MAX_LEN = 300;

/**
 * Build match chunks for one item, simulating backend behaviour:
 * 1. Find all hit positions in content.
 * 2. Expand each to [pos - CONTEXT_RADIUS, pos + queryLen + CONTEXT_RADIUS].
 * 3. Merge overlapping windows.
 * 4. For each merged window count how many query hits fall inside it.
 * 5. Slice to CHUNK_MAX_LEN, wrap each hit span with \u0001…\u0002 markers.
 */
function buildMatches(content: string, query: string): LoreMatchChunk[] {
  const lower = content.toLowerCase();
  const lq = query.toLowerCase();
  const qLen = query.length;

  // Collect all hit start indices (in the original content)
  const hits: number[] = [];
  let idx = lower.indexOf(lq);
  while (idx !== -1) {
    hits.push(idx);
    idx = lower.indexOf(lq, idx + 1);
  }
  if (hits.length === 0) return [];

  // Build windows [start, end) around each hit
  const windows = hits.map((h) => [
    Math.max(0, h - CONTEXT_RADIUS),
    Math.min(content.length, h + qLen + CONTEXT_RADIUS),
  ]);

  // Merge overlapping windows
  const merged: [number, number][] = [];
  let [cs, ce] = windows[0];
  for (let i = 1; i < windows.length; i++) {
    const [ws, we] = windows[i];
    if (ws <= ce) {
      ce = Math.max(ce, we);
    } else {
      merged.push([cs, ce]);
      cs = ws;
      ce = we;
    }
  }
  merged.push([cs, ce]);

  return merged.map(([start, end]) => {
    // Hits whose start falls inside this window
    const windowHits = hits.filter((h) => h >= start && h < end);
    const occurrenceCount = windowHits.length;

    // Trim window to CHUNK_MAX_LEN centred on the first hit
    let chunkStart = start;
    let chunkEnd = end;
    if (chunkEnd - chunkStart > CHUNK_MAX_LEN) {
      const mid = windowHits[0];
      chunkStart = Math.max(start, mid - Math.floor(CHUNK_MAX_LEN / 2));
      chunkEnd = Math.min(end, chunkStart + CHUNK_MAX_LEN);
    }

    // Re-collect hits that survived the trim
    const trimmedHits = windowHits.filter(
      (h) => h >= chunkStart && h + qLen <= chunkEnd,
    );

    // Build the matchContent string with \u0001…\u0002 markers around each hit.
    // Work from right-to-left so inserting markers doesn't shift earlier indices.
    let slice = content.slice(chunkStart, chunkEnd);
    // Adjust hit indices to be relative to chunkStart, then insert from end to start
    const relHits = trimmedHits.map((h) => h - chunkStart).reverse();
    for (const rel of relHits) {
      slice =
        slice.slice(0, rel) +
        HIGHLIGHT_START +
        slice.slice(rel, rel + qLen) +
        HIGHLIGHT_END +
        slice.slice(rel + qLen);
    }

    const leadEllipsis = chunkStart > 0 ? "…" : "";
    const trailEllipsis = chunkEnd < content.length ? "…" : "";

    return {
      matchContent: leadEllipsis + slice + trailEllipsis,
      occurrenceCount,
    };
  });
}

/**
 * Offset-cursor fulltext search over mock data.
 *
 * `offset` is treated as an opaque server-side DB cursor — the number of
 * matching items already consumed. The caller must never compute it themselves;
 * they must always use the `nextOffset` value returned by the previous call.
 *
 * Simulates network latency so skeleton and infinite-scroll can be tested.
 */
export async function searchLoreOffset(
  query: string,
  offset: number,
  size = 6,
): Promise<LoreSearchPage> {
  // Simulate network round-trip
  await new Promise((r) => setTimeout(r, 400));

  const q = query.trim().toLowerCase();
  if (!q) return { content: [], nextOffset: null, totalElements: 0 };

  // Build the full result set (represents all rows the DB would return)
  const allResults: LoreSearchResult[] = MOCK_LORE_ITEMS.filter((item) =>
    item.content.toLowerCase().includes(q),
  ).map((item) => {
    const matches = buildMatches(item.content, query.trim());
    const totalOccurrences = matches.reduce((s, m) => s + m.occurrenceCount, 0);
    return {
      id: item.id,
      title: item.title,
      byAdmin: item.byAdmin,
      secret: item.nonPublic,
      totalOccurrences,
      notAll: false,
      matches,
    };
  });

  const totalElements = allResults.length;

  // Slice using the opaque offset cursor
  const page = allResults.slice(offset, offset + size);

  // The server computes nextOffset — client must use this value verbatim
  const consumedSoFar = offset + page.length;
  const nextOffset: number | null =
    consumedSoFar < totalElements ? consumedSoFar : null;

  return { content: page, nextOffset, totalElements };
}
