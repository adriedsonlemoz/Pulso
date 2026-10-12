#!/usr/bin/env python3
"""Run actual Kotlin/JUnit without Gradle, using an explicitly supplied local toolchain.
The Android SDK, Compose, Room/KSP and instrumented tests are NOT validated here.
"""
import argparse
from pathlib import Path
import subprocess
import tempfile

parser=argparse.ArgumentParser()
parser.add_argument('--toolchain-dir',type=Path,required=True)
parser.add_argument('--audit-against',type=Path,help='Run only portable audit regressions against another source tree')
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
chain=args.toolchain_dir.resolve()
jars=[chain/n for n in ('kotlin-compiler-embeddable-2.1.20.jar','kotlin-stdlib-2.1.20.jar',
 'kotlin-script-runtime-2.1.20.jar','kotlin-reflect-1.6.10.jar','trove4j-1.0.20200330.jar',
 'kotlinx-coroutines-core-jvm-1.8.0.jar','annotations-13.0.jar','junit-4.13.2.jar','hamcrest-core-1.3.jar')]
assert all(p.is_file() for p in jars), 'Incomplete local toolchain; use Gradle instead.'
cp=':'.join(map(str,jars))
main_root=args.audit_against.resolve() if args.audit_against else root
sources=list((main_root/'core/src/main/kotlin').rglob('*.kt'))
if args.audit_against:
 tests=[root/'core/src/test/kotlin/br/com/pulsobateria/core/AuditRegressionTest.kt']
else:
 tests=list((root/'core/src/test/kotlin').rglob('*.kt'))
 tests+=list((root/'app/src/test/java').rglob('*.kt'))
 sources += [root/'app/src/main/java/br/com/pulsobateria/ui/Format.kt',
             root/'app/src/main/java/br/com/pulsobateria/monitor/NotificationText.kt']
import re
classes=[re.search(r'package (\S+)',p.read_text())[1]+'.'+re.search(r'class (\w+)',p.read_text())[1] for p in tests]
with tempfile.TemporaryDirectory(prefix='pulso-jvm-') as directory:
 output=Path(directory)/'tests.jar'
 compile_cmd=['java','-cp',cp,'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect',
              '-jvm-target','17','-classpath',cp,'-d',str(output),*map(str,sources+tests)]
 print('Kotlin 2.1.20 / JVM target 17 / JUnit 4.13.2',flush=True)
 print('Source tree:',main_root.name,'; test classes:',', '.join(classes),flush=True)
 subprocess.run(compile_cmd,check=True)
 result=subprocess.run(['java','-cp',str(output)+':'+cp,'org.junit.runner.JUnitCore',*classes])
 raise SystemExit(result.returncode)
