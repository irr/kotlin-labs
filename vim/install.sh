#!/bin/bash
git clone https://github.com/udalov/kotlin-vim.git
cd kotlin-vim
mkdir -p ~/.vim/{syntax,indent,ftdetect}
cp syntax/kotlin.vim ~/.vim/syntax/kotlin.vim
cp indent/kotlin.vim ~/.vim/indent/kotlin.vim
cp ftdetect/kotlin.vim ~/.vim/ftdetect/kotlin.vim
cd ..
rm -rf kotlin-vim

