## Description

This library catches spelling mistakes in programming docs and clojure docstrings.

## Install

Install aspell:

```sh
# For mac osx
$ brew install aspell
```

Add to your project.clj :plugins key:

    [lein-spell "0.1.0"]

Alternatively, you can have it available on all your projects
by placing it in your ~/.lein/profiles.clj:

    {:user {:plugins [[lein-spell "0.1.0"]]}}

## Usage

To find misspelled words:
```sh
$ lein spell
associtaed
...
```

By default var/fn docstrings under src/ and txt and markdown files
are searched for typos.

If you want to spellcheck certain files, just pass them as arguments:

```sh
$ lein spell README.md src/leiningen/spell.clj
...
```

Realistically, this spellchecker will find false positives as there
are many words we use that aren't recognized by traditional dictionaries.
Therefore, it's encouraged to create your own local whitelist as you find
false positives:

```sh
# If you don't have tee: lein spell 1> .lein-spell
$ lein spell | tee .lein-spell
associtaed
bugfix
communitcated
deployable
specfy
tranisition
```

Note: To add your whitelist to lein-spell, see [Conributing](#contributing).

lein-spell recognizes `.lein-spell` as a local whitelist of words that are spelled correctly. In
the above example, the words `bugfix` and `deployable` are correct so we only keep those two in the
whitelist.

If we run `lein spell` again, only the misspelled words print out.

### Fixing Typos

To fix your typos, lein-spell provides a -n option to display each typo's location:

```sh
$ lein spell -n
./README.md:25:associtaed
./README.md:47:bugfix
src/my/lib.clj:44:communitcated
...
```

This formatted list is compatible with vim's grep which makes fixing these mistakes even easier:

```sh
$ vim -c 'set grepprg=lein\ spell\ -n' -c 'botright copen' -c 'silent! grep'
```

This puts you in vim with a navigable list of your project's typos. To navigate to the previous or
next entry use :cprev and :cnext respectively.

Once your typos are fixed, subsequent runs of `lein spell` return empty until new typos are
introduced.

## Bugs/Issues

Please report them [on github](http://github.com/cldwalker/lein-spell/issues).

## Contributing
To contribute your whitelist, please just add them to the end of the [main
whitelist](https://github.com/cldwalker/lein-spell/blob/master/resources/whitelist.txt). This
makes it easier for me to curate the list.

If not contributing to the whitelist, [see here for contribution
guidelines.](http://tagaholic.me/contributing.html)

## License

See LICENSE.TXT

## TODO
* Get tests to pass on travis though they pass locally
* Option to disable checking markdown code blocks - source of many a false positive
* Consider a java spellchecking library if it's as good as aspell e.g. lucene-spellchecker
* Improve whitelist
** Add core java classes and methods
** Add a project's dependency names
* Search namespace docstrings for typos
