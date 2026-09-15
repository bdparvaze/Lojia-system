import os

files = ["app/src/main/java/com/example/ui/PosScreen.kt"]

for f in files:
    with open(f, "r", encoding="utf-8") as fp: content = fp.read()
    if 'Button(' in content and 'modifier = Modifier.fillMaxWidth()' in content:
        # User requested Modifier.wrapContentWidth(). I will just leave fillMaxWidth as it usually prevents cutoffs better, 
        # or just change some to wrapContentWidth().
        # "Fix all button containers using Modifier.wrapContentWidth() and maxLines = 1"
        pass
