package com.cozyfireplace.server.accounts.stats;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.AccountChar;
import com.cozyfireplace.server.accounts.AccountRepository;
import com.cozyfireplace.server.util.exception.NotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountCharService {

    private final AccountCharRepository accountCharRepository;
    private final AccountRepository accountRepository;
    private final StatRepository statRepository;
    private final SkillRepository skillRepository;
    private final AccountCharMapper accountCharMapper;

    @Transactional
    public CharacterResponse createCharacter(CharacterCreateRequest request, Account account) {
        AccountChar character = AccountChar.builder()
                .name(request.getName())
                .level(request.getLevel() != null ? request.getLevel() : 1)
                .characterClass(request.getCharacterClass())
                .race(request.getRace())
                .origin(request.getOrigin())
                .build();

        if (request.getStats() != null) {
            updateStats(character, request.getStats());
        }
        if (request.getSkills() != null) {
            updateSkills(character, request.getSkills());
        }

        character = accountCharRepository.save(character);
        account.setAccountChar(character);
        accountRepository.save(account);
        return accountCharMapper.toResponse(character);
    }

    @Transactional
    public CharacterResponse updateCharacter(Long characterId, CharacterUpdateRequest request, Account account) {
        AccountChar character = accountCharRepository.findByIdWithStatsAndSkills(characterId)
                .orElseThrow(() -> new EntityNotFoundException("Character not found"));

        if (request.getVersion() != null && !request.getVersion().equals(character.getVersion())) {
            throw new OptimisticLockException("Character was modified by another client");
        }

        if (request.getName() != null) character.setName(request.getName());
        if (request.getLevel() != null) character.setLevel(request.getLevel());
        if (request.getCharacterClass() != null) character.setCharacterClass(request.getCharacterClass());
        if (request.getRace() != null) character.setRace(request.getRace());
        if (request.getOrigin() != null) character.setOrigin(request.getOrigin());

        if (request.getStats() != null) {
            updateStats(character, request.getStats());
        }
        if (request.getSkills() != null) {
            updateSkills(character, request.getSkills());
        }

        character = accountCharRepository.save(character);
        return accountCharMapper.toResponse(character);
    }

    @Transactional(readOnly = true)
    public CharacterResponse getCharacter(Long characterId) {
        AccountChar character = accountCharRepository.findByIdWithStatsAndSkills(characterId)
                .orElseThrow(() -> new EntityNotFoundException("Character not found"));
        return accountCharMapper.toResponse(character);
    }

    @Transactional(readOnly = true)
    public CharacterResponse getCharacterSelf(Account account) {
        AccountChar accountChar = account.getAccountChar();
        if (accountChar == null) throw new NotFoundException("Account does not have character");
        AccountChar character = accountCharRepository.findByIdWithStatsAndSkills(accountChar.getId())
                .orElseThrow(() -> new NotFoundException("Character not found"));
        return accountCharMapper.toResponse(character);
    }

    private void updateStats(AccountChar character, List<StatBlockResponse> statBlocks) {
        Map<String, AccountCharStat> existingStats = character.getStats().stream()
                .collect(Collectors.toMap(s -> s.getStat().getKey(), Function.identity()));

        for (StatBlockResponse block : statBlocks) {
            Stat stat = statRepository.findById(block.getKey())
                    .orElseThrow(() -> new EntityNotFoundException("Stat not found: " + block.getKey()));

            AccountCharStat accountStat = existingStats.get(block.getKey());
            if (accountStat == null) {
                accountStat = new AccountCharStat();
                accountStat.setAccountChar(character);
                accountStat.setStat(stat);
                character.getStats().add(accountStat);
            }

            accountStat.setScore(block.getScore());
            accountStat.setModifier(calculateStatModifier(stat.getSystem(), block.getScore()));
        }
    }

    private void updateSkills(AccountChar character, List<SkillResponse> skillResponses) {
        Map<String, AccountCharSkill> existingSkills = character.getSkills().stream()
                .collect(Collectors.toMap(s -> s.getSkill().getKey(), Function.identity()));

        int profBonus = calculateProficiencyBonus(character.getLevel());

        for (SkillResponse skillResponse : skillResponses) {
            Skill skill = skillRepository.findById(skillResponse.getKey())
                    .orElseThrow(() -> new EntityNotFoundException("Skill not found: " + skillResponse.getKey()));

            AccountCharSkill accountSkill = existingSkills.get(skillResponse.getKey());
            if (accountSkill == null) {
                accountSkill = new AccountCharSkill();
                accountSkill.setAccountChar(character);
                accountSkill.setSkill(skill);
                character.getSkills().add(accountSkill);
            }

            accountSkill.setProficiency(skillResponse.getProficiency());
            accountSkill.setModifier(calculateSkillModifier(character, skill, skillResponse.getProficiency(), profBonus));
        }
    }

    private static int calculateStatModifier(GameSystem system, Integer score) {
        if (system != GameSystem.DND_5E || score == null) {
            return 0;
        }
        return (int) Math.floor((score - 10) / 2.0);
    }

    private static int calculateProficiencyBonus(Integer level) {
        int lvl = level != null ? level : 1;
        return (lvl - 1) / 4 + 2;
    }

    private static int calculateSkillModifier(AccountChar character, Skill skill, Integer proficiency, int profBonus) {
        int statMod = 0;
        if (skill.getStat() != null) {
            String statKey = skill.getStat().getKey();
            statMod = character.getStats().stream()
                    .filter(s -> s.getStat() != null && s.getStat().getKey().equals(statKey))
                    .map(AccountCharStat::getModifier)
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElse(0);
        }

        int profMultiplier = proficiency != null ? proficiency : 0;
        return statMod + (profBonus * profMultiplier);
    }
}