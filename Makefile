.DEFAULT_GOAL := update_run

SERVER_PATH ?=../../meandtheboys/Minecraft_Server
# Grab version number from plugin.yml
# The version we want is the first one, not the one called "api-version"
VERSION := $(shell grep version: src/main/resources/plugin.yml | head -n 1 | cut -d ' ' -f 2)

update_run: update run

update:
	# Remove old version of file starting with "fantasy-costco"
	rm -f $(SERVER_PATH)/plugins/fantasy-costco*
	# Print out the version number
	@echo "Version: $(VERSION)"
	# Replace build.gradle line containing "version 'x.x.x'" with "version:'$(VERSION)'"
	# but not if there is a space before the version
	sed -i -E "s/^(\s*)version '.*'/version '$(VERSION)'/" build.gradle
	# Build the plugin
	./gradlew build
	# Copy the plugin to the server
	cp build/libs/fantasy-costco-$(VERSION).jar $(SERVER_PATH)/plugins

run:
	# Run the server
	$(SERVER_PATH)/server.sh