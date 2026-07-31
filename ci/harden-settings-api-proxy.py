#!/usr/bin/env python3
from pathlib import Path

path = Path(__file__).resolve().parents[1] / "jars" / "Fixer.java"
text = path.read_text(encoding="utf-8")

old_catch = """                            } catch (java.lang.reflect.InvocationTargetException invokeError) {
                                Throwable cause = invokeError.getCause();"""
new_catch = """                            } catch (Throwable invokeError) {
                                // CheerpJ can surface the delegate's exception directly instead of
                                // wrapping it in InvocationTargetException. Handle both forms.
                                Throwable cause = invokeError;
                                if (invokeError instanceof java.lang.reflect.InvocationTargetException) {
                                    Throwable reflectedCause =
                                            ((java.lang.reflect.InvocationTargetException) invokeError).getCause();
                                    if (reflectedCause != null) {
                                        cause = reflectedCause;
                                    }
                                }"""

count = text.count(old_catch)
if count != 1:
    raise RuntimeError(f"SettingsAPI proxy catch block: expected one match, found {count}")

path.write_text(text.replace(old_catch, new_catch, 1), encoding="utf-8", newline="\n")
print("Hardened SettingsAPI proxy for direct CheerpJ delegate exceptions")
