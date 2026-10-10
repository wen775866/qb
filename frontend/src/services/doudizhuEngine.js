export const SUITS = {
  DIAMOND: { name: '方块', symbol: '♦', isRed: true, weight: 1 },
  CLUB: { name: '梅花', symbol: '♣', isRed: false, weight: 2 },
  HEART: { name: '红心', symbol: '♥', isRed: true, weight: 3 },
  SPADE: { name: '黑桃', symbol: '♠', isRed: false, weight: 4 },
  JOKER: { name: '王', symbol: '★', isRed: false, weight: 5 }
};

export const RANKS = [
  { text: '3', weight: 3 },
  { text: '4', weight: 4 },
  { text: '5', weight: 5 },
  { text: '6', weight: 6 },
  { text: '7', weight: 7 },
  { text: '8', weight: 8 },
  { text: '9', weight: 9 },
  { text: '10', weight: 10 },
  { text: 'J', weight: 11 },
  { text: 'Q', weight: 12 },
  { text: 'K', weight: 13 },
  { text: 'A', weight: 14 },
  { text: '2', weight: 15 }
];

export const HAND_TYPES = {
  INVALID: { id: 0, name: '无效牌型', power: 0 },
  SINGLE: { id: 1, name: '单张', power: 1 },
  PAIR: { id: 2, name: '对子', power: 2 },
  TRIPLET: { id: 3, name: '三张', power: 3 },
  TRIPLET_WITH_ONE: { id: 4, name: '三带一', power: 4 },
  TRIPLET_WITH_PAIR: { id: 5, name: '三带二', power: 5 },
  STRAIGHT: { id: 6, name: '顺子', power: 6 },
  CONSECUTIVE_PAIRS: { id: 7, name: '连对', power: 7 },
  AIRPLANE: { id: 8, name: '飞机', power: 8 },
  AIRPLANE_WITH_WINGS: { id: 9, name: '飞机带翅膀', power: 9 },
  FOUR_WITH_TWO: { id: 10, name: '四带二', power: 10 },
  BOMB: { id: 100, name: '炸弹', power: 100 },
  ROCKET: { id: 1000, name: '王炸', power: 1000 }
};

export function getCardKey(suitKey, rankText, isJoker, weight) {
  if (isJoker) {
    return weight === 17 ? 'joker_big' : 'joker_small';
  }
  return `${suitKey.toLowerCase()}_${rankText}`;
}

export function create54Deck() {
  const cards = [];
  let id = 1;
  for (const rank of RANKS) {
    for (const suitKey of ['SPADE', 'HEART', 'CLUB', 'DIAMOND']) {
      const key = getCardKey(suitKey, rank.text, false, rank.weight);
      cards.push({
        id: id++,
        suit: SUITS[suitKey],
        suitKey: suitKey.toLowerCase(),
        rank: rank.text,
        weight: rank.weight,
        isJoker: false,
        isRed: SUITS[suitKey].isRed,
        key: key,
        svgUrl: `/cards/${key}.svg`
      });
    }
  }
  cards.push({
    id: id++,
    suit: SUITS.JOKER,
    suitKey: 'joker',
    rank: '小王',
    weight: 16,
    isJoker: true,
    isRed: false,
    key: 'joker_small',
    svgUrl: '/cards/joker_small.svg'
  });
  cards.push({
    id: id++,
    suit: SUITS.JOKER,
    suitKey: 'joker',
    rank: '大王',
    weight: 17,
    isJoker: true,
    isRed: true,
    key: 'joker_big',
    svgUrl: '/cards/joker_big.svg'
  });
  return cards;
}

export function sortCards(cards) {
  return [...cards].sort((a, b) => {
    if (b.weight !== a.weight) return b.weight - a.weight;
    return b.suit.weight - a.suit.weight;
  });
}

export function createUnshuffledDeals() {
  const deck = create54Deck();
  const chunks = [];
  for (let i = 0; i < deck.length; i += 4) {
    chunks.push(deck.slice(i, i + 4));
  }
  chunks.sort(() => Math.random() - 0.5);
  const flattened = chunks.flat();
  return {
    hand0: sortCards(flattened.slice(0, 17)),
    hand1: sortCards(flattened.slice(17, 34)),
    hand2: sortCards(flattened.slice(34, 51)),
    bottom: flattened.slice(51, 54)
  };
}

export function createStandardDeals() {
  const deck = create54Deck();
  deck.sort(() => Math.random() - 0.5);
  return {
    hand0: sortCards(deck.slice(0, 17)),
    hand1: sortCards(deck.slice(17, 34)),
    hand2: sortCards(deck.slice(34, 51)),
    bottom: deck.slice(51, 54)
  };
}

export function parseCombo(cards) {
  if (!cards || cards.length === 0) return null;
  const sorted = sortCards(cards);
  const size = sorted.length;

  if (size === 2 && sorted.some(c => c.weight === 16) && sorted.some(c => c.weight === 17)) {
    return { type: HAND_TYPES.ROCKET, mainWeight: 100, length: 2, cards: sorted };
  }

  const counts = {};
  for (const c of sorted) counts[c.weight] = (counts[c.weight] || 0) + 1;
  const uniqueWeights = Object.keys(counts).map(Number).sort((a, b) => b - a);
  const countValues = Object.values(counts);

  if (size === 1) {
    return { type: HAND_TYPES.SINGLE, mainWeight: sorted[0].weight, length: 1, cards: sorted };
  }
  if (size === 2 && uniqueWeights.length === 1) {
    return { type: HAND_TYPES.PAIR, mainWeight: sorted[0].weight, length: 2, cards: sorted };
  }
  if (size === 3 && uniqueWeights.length === 1) {
    return { type: HAND_TYPES.TRIPLET, mainWeight: sorted[0].weight, length: 3, cards: sorted };
  }
  if (size === 4 && uniqueWeights.length === 1) {
    return { type: HAND_TYPES.BOMB, mainWeight: sorted[0].weight, length: 4, cards: sorted };
  }
  if (size === 4 && countValues.includes(3)) {
    const tripWeight = Number(Object.keys(counts).find(k => counts[k] === 3));
    return { type: HAND_TYPES.TRIPLET_WITH_ONE, mainWeight: tripWeight, length: 4, cards: sorted };
  }
  if (size === 5 && countValues.includes(3) && countValues.includes(2)) {
    const tripWeight = Number(Object.keys(counts).find(k => counts[k] === 3));
    return { type: HAND_TYPES.TRIPLET_WITH_PAIR, mainWeight: tripWeight, length: 5, cards: sorted };
  }
  if (size >= 5 && countValues.every(c => c === 1) && sorted.every(c => c.weight < 15)) {
    const weights = sorted.map(c => c.weight);
    let consecutive = true;
    for (let i = 0; i < weights.length - 1; i++) {
      if (weights[i] - weights[i + 1] !== 1) { consecutive = false; break; }
    }
    if (consecutive) {
      return { type: HAND_TYPES.STRAIGHT, mainWeight: weights[0], length: size, cards: sorted };
    }
  }
  if (size >= 6 && size % 2 === 0 && countValues.every(c => c === 2) && sorted.every(c => c.weight < 15)) {
    let consecutive = true;
    for (let i = 0; i < uniqueWeights.length - 1; i++) {
      if (uniqueWeights[i] - uniqueWeights[i + 1] !== 1) { consecutive = false; break; }
    }
    if (consecutive && uniqueWeights.length >= 3) {
      return { type: HAND_TYPES.CONSECUTIVE_PAIRS, mainWeight: uniqueWeights[0], length: size, cards: sorted };
    }
  }
  if (size === 6 && countValues.includes(4)) {
    const quadWeight = Number(Object.keys(counts).find(k => counts[k] === 4));
    return { type: HAND_TYPES.FOUR_WITH_TWO, mainWeight: quadWeight, length: 6, cards: sorted };
  }
  if (size === 8 && countValues.includes(4) && countValues.filter(c => c === 2).length === 2) {
    const quadWeight = Number(Object.keys(counts).find(k => counts[k] === 4));
    return { type: HAND_TYPES.FOUR_WITH_TWO, mainWeight: quadWeight, length: 8, cards: sorted };
  }

  // Airplane
  const trips = uniqueWeights.filter(w => counts[w] >= 3 && w < 15);
  if (trips.length >= 2) {
    let consecutiveTrips = [trips[0]];
    for (let i = 0; i < trips.length - 1; i++) {
      if (trips[i] - trips[i + 1] === 1) consecutiveTrips.push(trips[i + 1]);
      else {
        if (consecutiveTrips.length >= 2) break;
        consecutiveTrips = [trips[i + 1]];
      }
    }
    const numTrips = consecutiveTrips.length;
    if (numTrips >= 2) {
      if (size === numTrips * 3) return { type: HAND_TYPES.AIRPLANE, mainWeight: consecutiveTrips[0], length: size, cards: sorted };
      if (size === numTrips * 4) return { type: HAND_TYPES.AIRPLANE_WITH_WINGS, mainWeight: consecutiveTrips[0], length: size, cards: sorted };
      if (size === numTrips * 5 && countValues.filter(c => c === 2).length >= numTrips) {
        return { type: HAND_TYPES.AIRPLANE_WITH_WINGS, mainWeight: consecutiveTrips[0], length: size, cards: sorted };
      }
    }
  }

  return null;
}

export function canBeat(lastCombo, candidate) {
  if (!lastCombo) return true;
  if (candidate.type.id === HAND_TYPES.ROCKET.id) return true;
  if (lastCombo.type.id === HAND_TYPES.ROCKET.id) return false;
  if (candidate.type.id === HAND_TYPES.BOMB.id) {
    if (lastCombo.type.id !== HAND_TYPES.BOMB.id) return true;
    return candidate.mainWeight > lastCombo.mainWeight;
  }
  if (candidate.type.id !== lastCombo.type.id || candidate.length !== lastCombo.length) {
    return false;
  }
  return candidate.mainWeight > lastCombo.mainWeight;
}

export function findAllValidMoves(hand, lastCombo) {
  const sorted = sortCards(hand);
  const results = [];

  if (!lastCombo) {
    const seen = new Set();
    for (const c of sorted) {
      if (!seen.has(c.weight)) {
        seen.add(c.weight);
        results.push([c]);
      }
    }
    const counts = {};
    for (const c of sorted) counts[c.weight] = (counts[c.weight] || 0) + 1;
    for (const [w, count] of Object.entries(counts)) {
      if (count >= 2) results.push(sorted.filter(c => c.weight === Number(w)).slice(0, 2));
    }
    return results.slice(0, 5);
  }

  const counts = {};
  for (const c of sorted) counts[c.weight] = (counts[c.weight] || 0) + 1;

  if (lastCombo.type.id === HAND_TYPES.SINGLE.id) {
    const singles = sorted.filter(c => c.weight > lastCombo.mainWeight);
    const seen = new Set();
    for (const c of singles) {
      if (!seen.has(c.weight)) {
        seen.add(c.weight);
        results.push([c]);
      }
    }
  } else if (lastCombo.type.id === HAND_TYPES.PAIR.id) {
    for (const [w, count] of Object.entries(counts)) {
      if (count >= 2 && Number(w) > lastCombo.mainWeight) {
        results.push(sorted.filter(c => c.weight === Number(w)).slice(0, 2));
      }
    }
  } else if (lastCombo.type.id === HAND_TYPES.TRIPLET.id) {
    for (const [w, count] of Object.entries(counts)) {
      if (count >= 3 && Number(w) > lastCombo.mainWeight) {
        results.push(sorted.filter(c => c.weight === Number(w)).slice(0, 3));
      }
    }
  } else if (lastCombo.type.id === HAND_TYPES.TRIPLET_WITH_ONE.id) {
    for (const [w, count] of Object.entries(counts)) {
      if (count >= 3 && Number(w) > lastCombo.mainWeight) {
        const trip = sorted.filter(c => c.weight === Number(w)).slice(0, 3);
        const kicker = sorted.find(c => c.weight !== Number(w));
        if (kicker) results.push([...trip, kicker]);
      }
    }
  } else if (lastCombo.type.id === HAND_TYPES.TRIPLET_WITH_PAIR.id) {
    for (const [w, count] of Object.entries(counts)) {
      if (count >= 3 && Number(w) > lastCombo.mainWeight) {
        const trip = sorted.filter(c => c.weight === Number(w)).slice(0, 3);
        const pairKey = Object.keys(counts).find(k => Number(k) !== Number(w) && counts[k] >= 2);
        if (pairKey) {
          const pair = sorted.filter(c => c.weight === Number(pairKey)).slice(0, 2);
          results.push([...trip, ...pair]);
        }
      }
    }
  }

  if (lastCombo.type.id !== HAND_TYPES.ROCKET.id) {
    for (const [w, count] of Object.entries(counts)) {
      if (count === 4 && (lastCombo.type.id !== HAND_TYPES.BOMB.id || Number(w) > lastCombo.mainWeight)) {
        results.push(sorted.filter(c => c.weight === Number(w)));
      }
    }
  }

  if (sorted.some(c => c.weight === 16) && sorted.some(c => c.weight === 17)) {
    results.push(sorted.filter(c => c.isJoker));
  }

  return results;
}

export function findAiMove(hand, lastCombo, isTeammate, teammateCardCount) {
  if (isTeammate && teammateCardCount <= 2 && lastCombo) {
    return null;
  }
  const moves = findAllValidMoves(hand, lastCombo);
  if (!moves || moves.length === 0) return null;
  if (!lastCombo) return moves.sort((a, b) => a[0].weight - b[0].weight)[0];
  if (isTeammate) {
    const nonBombs = moves.filter(m => {
      const parsed = parseCombo(m);
      return parsed.type.id !== HAND_TYPES.BOMB.id && parsed.type.id !== HAND_TYPES.ROCKET.id;
    });
    if (nonBombs.length > 0) return nonBombs[0];
    return null;
  }
  return moves[0];
}
