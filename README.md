# TimeProNorm

"""
cat input_file.txp | java -cp "lib/scala-library.jar:lib/timenorm-0.9.1-SNAPSHOT.jar:lib/threetenbp-0.8.1.jar:lib/TimeProNorm_v2.6.jar" eu.fbk.timePro.TimeProNormApply output_file.txp
"""

The input file should contain 6 columns:
- token
- PoS
- lemma
- entity
- chunk
- tags (timex rules)
- tags (timex rules)
- Timex (B-DATE, B-DURATION, B-TIME, I-DATE, I-DURATION, I-TIME)
