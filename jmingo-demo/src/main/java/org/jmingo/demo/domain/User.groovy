package org.jmingo.demo.domain

import org.jmingo.document.annotation.Document;

@Document(collectionName = "userCollection")
class User extends BaseDocument {
    String login;
    String password;
    Person person;
    List<String> accounts
    List<Group> groups

    @Override
    public String toString() {
        return """\
                User{
                    id = '$id'
                    login='$login',
                    password='$password',
                    person=$person,
                    accounts=$accounts,
                }"""
    }
}
