# create a compressed archive that has a depth of six
# compressed archive members
mkdir -p test-with-depth-six/A/B/C/D/E/F
echo foo > test-with-depth-six/A/B/C/D/E/F/foo
tar -cvzf test-with-depth-six/A/B/C/D/E/F.tgz -C test-with-depth-six/A/B/C/D/E  --exclude ".DS_Store" F/foo
rm -rf test-with-depth-six/A/B/C/D/E/F
tar -cvzf test-with-depth-six/A/B/C/D/E.tgz -C test-with-depth-six/A/B/C/D --exclude ".DS_Store" E/F.tgz
rm -rf test-with-depth-six/A/B/C/D/E
tar -cvzf test-with-depth-six/A/B/C/D.tgz -C test-with-depth-six/A/B/C --exclude ".DS_Store" D/E.tgz
rm -rf test-with-depth-six/A/B/C/D
tar -cvzf test-with-depth-six/A/B/C.tgz -C test-with-depth-six/A/B --exclude ".DS_Store" C/D.tgz
rm -rf test-with-depth-six/A/B/C
tar -cvzf test-with-depth-six/A/B.tgz -C test-with-depth-six/A --exclude ".DS_Store" B/C.tgz
rm -rf test-with-depth-six/A/B
tar -cvzf test-with-depth-six/A.tgz -C test-with-depth-six --exclude ".DS_Store" A/B.tgz
rm -rf test-with-depth-six/A
tar -cvzf test-with-depth-six.tgz --exclude ".DS_Store" test-with-depth-six/A.tgz
rm -rf test-with-depth-six
