# create a compressed archive that has a depth of six
# compressed archive members
mkdir -p test-with-depth-six/A/B/C/D/E/F
cd test-with-depth-six/A/B/C/D/E/F
echo foo > foo
cd ..
tar -cvzf F.tgz --exclude ".DS_Store" F/foo
rm -rf F
cd ..
tar -cvzf E.tgz --exclude ".DS_Store" E/*
cd ..
rm -rf E
tar -cvzf D.tgz --exclude ".DS_Store" D/*
cd ..
rm -rf D
tar -cvzf C.tgz --exclude ".DS_Store" C/*
cd ..
rm -rf C
tar -cvzf B.tgz --exclude ".DS_Store" B/*
cd ..
rm -rf B
tar -cvzf A.tgz --exclude ".DS_Store" A/*
cd ..
rm -rf A
tar -cvzf test-with-depth-six.tgz --exclude ".DS_Store" test-with-depth-six/*
rm -rf test-with-depth-six
