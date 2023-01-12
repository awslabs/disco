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
