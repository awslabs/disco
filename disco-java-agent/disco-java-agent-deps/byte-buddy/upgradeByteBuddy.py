# Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
#   Licensed under the Apache License, Version 2.0 (the "License").
#   You may not use this file except in compliance with the License.
#   A copy of the License is located at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   or in the "license" file accompanying this file. This file is distributed
#   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
#   express or implied. See the License for the specific language governing
#   permissions and limitations under the License.

import os
import sys
import shutil
from subprocess import check_call

def download_byte_buddy(version):
    """
    Downloads ByteBuddy release from GitHub

    Parameters:
        version (string): Version number for release to download
    """

    check_call(["wget", f"https://github.com/raphw/byte-buddy/archive/refs/tags/byte-buddy-{version}.zip"])
    check_call(["unzip", "-q", f"byte-buddy-{version}.zip"])

def patch_byte_buddy(version):
    """
    Applies patch files to ByteBuddy source

    Parameters:
        version (string): Version number for release being patched
    """

    for patch in os.listdir("patches"):
        check_call(f"patch -p1 < ../patches/{patch}", cwd=f"./byte-buddy-byte-buddy-{version}", shell=True)

def build_byte_buddy(version):
    """
    Builds local copy of ByteBuddy

    Parameters:
        version (string): Version number to be built
    """

    check_call(["./mvnw", "package", "-P", "extras"], cwd=f"./byte-buddy-byte-buddy-{version}")

def copy_byte_buddy_artifacts(version):
    """
    Copies built ByteBuddy artifacts to byte-buddy dir

    Parameters:
        version (string): Version number of built artifacts
    """

    for extension in [".jar", "-javadoc.jar", "-sources.jar"]:
        os.rename(f"./byte-buddy-byte-buddy-{version}/byte-buddy-dep/target/byte-buddy-dep-{version}{extension}", f"./byte-buddy-dep-{version}{extension}")
        os.rename(f"./byte-buddy-byte-buddy-{version}/byte-buddy-agent/target/byte-buddy-agent-{version}{extension}", f"./byte-buddy-agent-{version}{extension}")

def cleanup(version):
    """
    Cleans up downloaded zip file and folders

    Parameters:
        version (string): Version number of files to be cleaned
    """

    shutil.rmtree(f"byte-buddy-byte-buddy-{version}")
    os.remove(f"byte-buddy-{version}.zip")

if __name__ == "__main__":
    try:
        _, version = sys.argv
    except ValueError:
        sys.exit(f'Usage: {sys.argv[0]} BB-VERSION\n  (BB-VERSION example: 1.12.6)')

    download_byte_buddy(version)
    patch_byte_buddy(version)
    build_byte_buddy(version)
    copy_byte_buddy_artifacts(version)
    cleanup(version)
