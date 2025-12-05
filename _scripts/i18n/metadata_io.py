#!/usr/bin/env python3
"""
Shared library for reading/writing split metadata files.

This module provides a unified interface for accessing string metadata
that is split across multiple category-based YAML files. It handles:
- Loading the index to map string keys to category files
- Loading defaults and merging with category-specific metadata
- Converting between YAML and Python dictionaries
- Providing efficient access patterns (load all, load by key, load by category)
"""

import json
import yaml
from pathlib import Path
from typing import Dict, List, Optional, Set
from datetime import datetime


class MetadataIO:
    """Handles reading and writing split metadata files."""

    def __init__(self, i18n_dir: Path, metadata_subdir: str = "metadata_divesms"):
        """
        Initialize MetadataIO.

        Args:
            i18n_dir: Path to _scripts/i18n/ directory
            metadata_subdir: Name of metadata subdirectory (e.g., 'metadata_divesms', 'metadata_main')
        """
        self.i18n_dir = Path(i18n_dir)
        self.metadata_dir = self.i18n_dir / metadata_subdir
        self.index_path = self.metadata_dir / "index.json"
        self.defaults_path = self.metadata_dir / "defaults.json"

        # Cached data
        self._index: Optional[Dict] = None
        self._defaults: Optional[Dict] = None
        self._category_cache: Dict[str, Dict] = {}

    def _load_index(self) -> Dict:
        """Load index.json (string key → category mapping)."""
        if self._index is None:
            if not self.index_path.exists():
                raise FileNotFoundError(f"Index file not found: {self.index_path}")

            with open(self.index_path, 'r', encoding='utf-8') as f:
                self._index = json.load(f)

        return self._index

    def _load_defaults(self) -> Dict:
        """Load defaults.json."""
        if self._defaults is None:
            if not self.defaults_path.exists():
                # Return empty defaults if file doesn't exist yet
                self._defaults = {}
            else:
                with open(self.defaults_path, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                    # Extract the nested "defaults" object
                    self._defaults = data.get('defaults', {})

        return self._defaults

    def _load_category_file(self, category: str) -> Dict:
        """
        Load a category YAML file.

        Args:
            category: Category name (e.g., 'authentication', 'trading')

        Returns:
            Dictionary of string metadata for this category
        """
        if category in self._category_cache:
            return self._category_cache[category]

        category_path = self.metadata_dir / f"{category}.yaml"

        if not category_path.exists():
            raise FileNotFoundError(f"Category file not found: {category_path}")

        with open(category_path, 'r', encoding='utf-8') as f:
            category_data = yaml.safe_load(f) or {}

        self._category_cache[category] = category_data
        return category_data

    def _merge_with_defaults(self, metadata: Dict) -> Dict:
        """
        Merge string metadata with defaults.

        Args:
            metadata: String-specific metadata

        Returns:
            Merged metadata with defaults applied
        """
        defaults = self._load_defaults()
        merged = {}

        # Deep merge defaults with metadata
        # Metadata values override defaults
        for key, default_value in defaults.items():
            if isinstance(default_value, dict):
                # Deep merge for nested dictionaries
                merged[key] = {**default_value, **metadata.get(key, {})}
            else:
                # Use metadata value if exists, otherwise default
                merged[key] = metadata.get(key, default_value)

        # Add any keys in metadata that aren't in defaults
        for key, value in metadata.items():
            if key not in merged:
                merged[key] = value

        return merged

    def get_string_metadata(self, string_key: str) -> Optional[Dict]:
        """
        Get metadata for a specific string key.

        Args:
            string_key: String resource key (e.g., 'flat_app_name')

        Returns:
            Metadata dictionary with defaults merged, or None if not found
        """
        index = self._load_index()

        # Find category for this string
        category = None
        for cat, keys in index.get('categories', {}).items():
            if string_key in keys:
                category = cat
                break

        if category is None:
            return None

        # Load category file
        category_data = self._load_category_file(category)

        # Get string metadata
        string_metadata = category_data.get(string_key)

        if string_metadata is None:
            return None

        # Merge with defaults
        return self._merge_with_defaults(string_metadata)

    def get_category_metadata(self, category: str) -> Dict[str, Dict]:
        """
        Get all metadata for a specific category.

        Args:
            category: Category name (e.g., 'authentication')

        Returns:
            Dictionary mapping string keys to their metadata (with defaults merged)
        """
        category_data = self._load_category_file(category)

        # Merge defaults for each string
        result = {}
        for string_key, metadata in category_data.items():
            result[string_key] = self._merge_with_defaults(metadata)

        return result

    def get_all_metadata(self) -> Dict[str, Dict]:
        """
        Get metadata for all strings across all categories.

        Returns:
            Dictionary mapping string keys to their metadata (with defaults merged)
        """
        index = self._load_index()
        all_metadata = {}

        # Load each category
        for category in index.get('categories', {}).keys():
            category_metadata = self.get_category_metadata(category)
            all_metadata.update(category_metadata)

        return all_metadata

    def get_all_string_keys(self) -> Set[str]:
        """
        Get all string keys across all categories.

        Returns:
            Set of all string keys
        """
        index = self._load_index()
        all_keys = set()

        for keys in index.get('categories', {}).values():
            all_keys.update(keys)

        return all_keys

    def get_categories(self) -> List[str]:
        """
        Get list of all categories.

        Returns:
            List of category names
        """
        index = self._load_index()
        return list(index.get('categories', {}).keys())

    def get_metadata_version(self) -> str:
        """Get metadata version from index."""
        index = self._load_index()
        return index.get('version', '1.0')

    def get_project_info(self) -> Dict:
        """Get project-level metadata from index."""
        index = self._load_index()
        return {
            'project': index.get('project', 'WizeUp Browser'),
            'default_locale': index.get('default_locale', 'en'),
            'last_updated': index.get('last_updated', ''),
            'metadata_version': index.get('version', '1.0')
        }

    def save_category(self, category: str, data: Dict[str, Dict]) -> None:
        """
        Save metadata for a category to its YAML file.

        Args:
            category: Category name
            data: Dictionary mapping string keys to their metadata (without defaults)
        """
        category_path = self.metadata_dir / f"{category}.yaml"

        # Ensure metadata directory exists
        self.metadata_dir.mkdir(parents=True, exist_ok=True)

        # Write YAML
        with open(category_path, 'w', encoding='utf-8') as f:
            yaml.dump(data, f, default_flow_style=False, allow_unicode=True, sort_keys=True)

        # Clear cache
        if category in self._category_cache:
            del self._category_cache[category]

    def save_index(self, index_data: Dict) -> None:
        """
        Save index.json.

        Args:
            index_data: Complete index dictionary
        """
        # Update timestamp
        index_data['last_updated'] = datetime.now().strftime('%Y-%m-%d')

        # Ensure metadata directory exists
        self.metadata_dir.mkdir(parents=True, exist_ok=True)

        # Write JSON
        with open(self.index_path, 'w', encoding='utf-8') as f:
            json.dump(index_data, f, indent=2, ensure_ascii=False)

        # Clear cache
        self._index = None

    def save_defaults(self, defaults_data: Dict) -> None:
        """
        Save defaults.json.

        Args:
            defaults_data: Defaults dictionary
        """
        # Ensure metadata directory exists
        self.metadata_dir.mkdir(parents=True, exist_ok=True)

        # Write JSON
        with open(self.defaults_path, 'w', encoding='utf-8') as f:
            json.dump(defaults_data, f, indent=2, ensure_ascii=False)

        # Clear cache
        self._defaults = None

    def is_split_format(self) -> bool:
        """Check if split metadata format exists."""
        return self.metadata_dir.exists() and self.index_path.exists()

    def is_legacy_format(self) -> bool:
        """Check if legacy strings_metadata.json exists."""
        legacy_path = self.i18n_dir / "strings_metadata.json"
        return legacy_path.exists()


def create_metadata_io(i18n_dir: Optional[Path] = None, metadata_subdir: str = "metadata_divesms") -> MetadataIO:
    """
    Factory function to create MetadataIO instance.

    Args:
        i18n_dir: Path to _scripts/i18n/ directory. If None, auto-detects.
        metadata_subdir: Name of metadata subdirectory (default: 'metadata_divesms')

    Returns:
        MetadataIO instance
    """
    if i18n_dir is None:
        # Auto-detect: assume this script is in _scripts/i18n/
        i18n_dir = Path(__file__).parent

    return MetadataIO(i18n_dir, metadata_subdir)


if __name__ == '__main__':
    # Simple test
    io = create_metadata_io()

    if io.is_split_format():
        print(f"✓ Split format detected")
        print(f"  Categories: {', '.join(io.get_categories())}")
        print(f"  Total strings: {len(io.get_all_string_keys())}")

        # Test loading a string
        metadata = io.get_string_metadata('flat_app_name')
        if metadata:
            print(f"  Test string 'flat_app_name': {metadata.get('category', 'N/A')}")
    else:
        print("✗ Split format not found. Run migration first.")

    if io.is_legacy_format():
        print("  Legacy strings_metadata.json still exists")
