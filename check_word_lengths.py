from collections import defaultdict
from typing import List


def join_words(words: List[str]) -> List[str]:
    lines = []
    # Join as many words as possible <= 15 chars
    line = ""
    while words:
        current_word = words.pop(0)
        # 14 chars + 1 space = 15 chars
        if len(line) + len(current_word) > 14:
            lines.append(line)
            line = ""
        line += f"{current_word} "
    lines.append(line)
    return lines


first_count = 0
second_count = 0
counter = defaultdict(lambda: 0)
with open("./src/main/resources/merchprices.yml", "r") as f:
    for readline in f:
        name, price = f.readline().split(":")
        # We only care about name, and we want to fit whole words
        # into chunks of 15 chars
        name = name.strip().replace("_", " ")
        name = name.title()
        words = name.split(" ")
        lines = join_words(words)
        if len(lines) <= 2:
            continue
        first_count += 1
        # These are in decreasing order of frequency
        # (e.g. polished shows up most, followed by slab, etc)
        # Ties were decided by choosing longest first
        name = name.replace("Polished", "Plshd")
        name = name.replace("Slab", "Slb")
        name = name.replace("Blackstone", "Blkstn")
        name = name.replace("Concrete", "Cncrt")
        name = name.replace("Weathered", "Wthrd")
        name = name.replace("Cobblestone", "Cblstn")
        name = name.replace("Stairs", "Strs")
        name = name.replace("Stained", "Stn")
        name = name.replace("Terracotta", "Trcta")
        name = name.replace("Deepslate", "Dpslt")
        words = name.split(" ")
        lines = join_words(words)
        if len(lines) <= 2:
            continue
        print(lines)
        print(name)
        for word in name.split(" "):
            counter[word] += 1
        second_count += 1

print(f"First count: {first_count}    (Allowed to be greater than 0)\n")
print(f"Second count: {second_count}    (Should be 0)\n")
print(
    f"Word counts: {dict(sorted(counter.items(), key=lambda item: item[1]))}" +
    "    (Should be empty)")
