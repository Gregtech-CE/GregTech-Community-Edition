# Contributing to GregTech Community Edition
First off, thank you for your interest in contributing! Remember every contribution counts.
#### [![Discord](https://img.shields.io/discord/432092648724103190.svg?colorB=7289DA&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAHYAAABWAgMAAABnZYq0AAAACVBMVEUAAB38%2FPz%2F%2F%2F%2Bm8P%2F9AAAAAXRSTlMAQObYZgAAAAFiS0dEAIgFHUgAAAAJcEhZcwAACxMAAAsTAQCanBgAAAAHdElNRQfhBxwQJhxy2iqrAAABoElEQVRIx7WWzdGEIAyGgcMeKMESrMJ6rILZCiiBg4eYKr%2Fd1ZAfgXFm98sJfAyGNwno3G9sLucgYGpQ4OGVRxQTREMDZjF7ILSWjoiHo1n%2BE03Aw8p7CNY5IhkYd%2F%2F6MtO3f8BNhR1QWnarCH4tr6myl0cWgUVNcfMcXACP1hKrGMt8wcAyxide7Ymcgqale7hN6846uJCkQxw6GG7h2MH4Czz3cLqD1zHu0VOXMfZjHLoYvsdd0Q7ZvsOkafJ1P4QXxrWFd14wMc60h8JKCbyQvImzlFjyGoZTKzohwWR2UzSONHhYXBQOaKKsySsahwGGDnb%2FiYPJw22sCqzirSULYy1qtHhXGbtgrM0oagBV4XiTJok3GoLoDNH8ooTmBm7ZMsbpFzi2bgPGoXWXME6XT%2BRJ4GLddxJ4PpQy7tmfoU2HPN6cKg%2BledKHBKlF8oNSt5w5g5o8eXhu1IOlpl5kGerDxIVT%2BztzKepulD8utXqpChamkzzuo7xYGk%2FkpSYuviLXun5bzdRf0Krejzqyz7Z3p0I1v2d6HmA07dofmS48njAiuMgAAAAASUVORK5CYII%3D)](https://discord.gg/Tp3yDnE)

## Want to contribute as player?
We appreciate feedback from our players as you are the reason why we are doing all of this. Without you, we would not be where we are.

### Found a bug?
* **Ensure the bug was not already reported** by searching on GitHub under [Issues](https://github.com/GregTechCE/GregTech/issues).
* If you're unable to find an open issue addressing the problem, [open a new one](https://github.com/GregTechCE/GregTech/issues/new/choose) using **Bug report** template.
Be sure to include a **title and clear description**, with as much relevant information as possible, let the template be your guide.

### Something is missing? Thought about a cool feature?
* Check if there is similar idea under [Issues](https://github.com/GregTechCE/GregTech/issues) and if so, feel free to add more information and thoughts to the issue.
* Nothing similar? Then [open a new issue](https://github.com/GregTechCE/GregTech/issues/new/choose) using the **Feature request** template.

### Have a question regarding project?
* We have a Discord (link above), feel free to hang out and ask your questions. There are always some community members which may provide answers.
* Discord not your thing? Okay, understandable, then please [open a new issue](https://github.com/GregTechCE/GregTech/issues/new/choose) using **Question** template.

### Anything else?
* Discord is right place for it.

## Want to contribute some more?
We are mostly organizing our self on Discord and it is preferred place to discuss issues or quickly exchange info.
GitHub is more of input, output and storage kind of place.
Of course if we made any decision regarding issues/PRs we will write it on GitHub, so it is available to anyone.

### Know a foreign language and want to contribute translation?
* If we already have language files for it feel free to review them and let us know any mistakes or improvements.
* If we don't have the language files, and you feel brave enough to make them, then please reach out to us on Discord, so we can discuss it.

### Don't know how to code?
We can still take your help.
* You can look through open issues with [status: unverified](https://github.com/GregTechCE/GregTech/labels/status%3A%20unverified) and confirm they are present.
On simplest possible configuration and write down reproduction steps if they are missing. Or provide any other related information.
* Or you can let us know what you think on issues with tag [open for discussion](https://github.com/GregTechCE/GregTech/labels/open%20for%20discussion).
* Maybe your expertise could help us on issues with [status: help needed](https://github.com/GregTechCE/GregTech/labels/status%3A%20help%20needed)

### Know how to mod or at least code?


## Tag explanation
Tags are used across issues and PRs to quickly determine what given items is about.
### Shared
#### integration
There is some integration to either CT, JEI or TOP.
#### subsystem
Defines related areas for this issue or PR. For example, an issue reporting problems with cables should be marked with subsystem: cables.
#### type: duplicate
This item duplicates some other.
They should be linked, if there is some useful information it should be transferred and this item should be closed.
#### status: accepted
For PR it means it will be included in next release of appropriate size.
For Issue it means it can be worked on. It is either small enough that requirements are known from the description, 
or it has abstract defining acceptance criteria and implementation specification.

### PR specific
#### release note needed
There is need for explanation of given feature/bug in release notes
#### rsr (Release size requirements)
Defines under which circumstances can be this included in release.
There are 4 sizes 
* Revision (small addition or transparent change without big effect on existing games)
* Minor (nondestructive or destructive must have change to API, change to recipes or mechanics) 
* Major (destructive change to API, save games compatibility problems, major mechanics rework)
* Undetermined (size of change is not known)
Currently, only first 2 mentioned are releasable.
#### status: release blocker
Next release can't get out without this PR.
#### early merge adept
Is given to PRs with big impact (mostly performance, or addons) which required more testing time or addons need more time to adjust to it
#### impact
Given if changes are expected to have impact that may create need to be handled.
For example: API changes, recipe changes, JEI category changes, etc...
Defines who will be affected by this change, options:
* Addons
* Modpacks

### Issue specific
#### question
This issue is question and should be close after answering it.
#### status: workaround exists
There is a known workaround for the given issue.
#### status: wip
This issue is getting prepared. For PRs there is draft function.


